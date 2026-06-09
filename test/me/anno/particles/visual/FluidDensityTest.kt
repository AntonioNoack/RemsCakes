package me.anno.particles.visual

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.engine.DefaultAssets
import me.anno.engine.ui.render.RenderMode
import me.anno.engine.ui.render.RenderMode.Companion.opaqueNodeSettings
import me.anno.engine.ui.render.RenderMode.Companion.thenBloomAndExposure
import me.anno.engine.ui.render.RenderState
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.GFXState
import me.anno.gpu.GFXState.timeRendering
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.blending.BlendMode
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.CompactAttributeLayout.Companion.bind
import me.anno.gpu.buffer.SimpleBuffer.Companion.flat01
import me.anno.gpu.buffer.StaticBuffer
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.shader.DepthTransforms
import me.anno.gpu.shader.DepthTransforms.depthVars
import me.anno.gpu.shader.DepthTransforms.rawToDepth
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.texture.CubemapTexture.Companion.cubemapsAreLeftHanded
import me.anno.gpu.texture.TextureLib
import me.anno.graph.visual.render.QuickPipeline
import me.anno.graph.visual.render.Texture
import me.anno.graph.visual.render.effects.FXAANode
import me.anno.graph.visual.render.effects.GizmoNode
import me.anno.graph.visual.render.effects.SSAONode
import me.anno.graph.visual.render.effects.SSRNode
import me.anno.graph.visual.render.scene.*
import me.anno.particles.FluidSimulationTests
import me.anno.particles.ParticleSet
import me.anno.particles.ParticleSet.Companion.mergeParticles
import me.anno.particles.utils.ParticlePhysics
import org.joml.Vector3f

// todo we need proper fluid rendering:
//  - put all particles on the GPU
//  - create a camera-aligned grid, maybe even make it perspective
//  - register particles in (all) relevant cells across multiple LODs
//  - into sparse grids, so we can check whether the particle is close enough

// or we could just rendering them in a compute-shader :3,
//  and we can limit their size there: if a particle is too big,
//  it should be put into a size-limited buffer for rasterization
//  -> ordering from front-to-back is an issue :/

// todo we don't have that many -> just use alpha-blending with a smart formula

class FluidDensityRenderNode(val particles: ParticleSet) : RenderViewNode(
    "FluidDensity", listOf(
        "Int", "Width",
        "Int", "Height",
        "Texture", "Illuminated",
        "Texture", "Depth",
    ), listOf(
        "Texture", "Illuminated",
        "Texture", "FluidDebug",
    )
) {

    // todo we need the following:
    //  uint32 buffer for each pixel for
    //   - collected density
    //   - R,G,B
    //   - normalU,V
    //  a shader to clear that buffer to zero (we have that, I think)
    //  a shader to accumulate rasterize/particles onto that buffer
    //  a post-process shader to mix the old image with the fluid
    //  a buffer for the particle data
    //   -> we have few particles, we could even sort them on CPU

    val accuShader = Shader(
        "fluid-accu", listOf(
            Variable(GLSLType.V2F, "positions", VariableMode.ATTR),
            Variable(GLSLType.V4F, "particlePositions", VariableMode.ATTR),
            Variable(GLSLType.V3F, "camDirU"),
            Variable(GLSLType.V3F, "camDirV"),
            Variable(GLSLType.V3F, "camDirW"),
            Variable(GLSLType.M4x4, "transform"),
        ), "" +
                "void main() {\n" +
                "   uvs = positions * 2.0 - 1.0;\n" +
                "   localPosition = particlePositions.xyz + (uvs.x * camDirU + uvs.y * camDirV) * particlePositions.w;\n" +
                "   gl_Position = transform * vec4(localPosition, 1.0);\n" +
                "   uv = gl_Position.xy / gl_Position.w * 0.5 + 0.5;\n" +
                "   float zx = dot(camDirW, localPosition);\n" +
                "   z0 = zx - particlePositions.w;\n" +
                "   z1 = zx + particlePositions.w;\n" +
                "}\n", listOf(
            Variable(GLSLType.V3F, "localPosition"),
            Variable(GLSLType.V2F, "uvs"),
            Variable(GLSLType.V2F, "uv"),
            Variable(GLSLType.V1F, "z0"),
            Variable(GLSLType.V1F, "z1"),
        ), listOf(
            Variable(GLSLType.S2D, "depthTex"),
            Variable(GLSLType.V4F, "result", VariableMode.OUT)
        ) + depthVars, "" + rawToDepth +
                "void main() {\n" +
                // depth-occlusion check
                "   float rawDepth = texture(depthTex,uv).x;\n" +
                "   float depth = rawToDepth(rawDepth);\n" +
                "   if (depth < z0) discard;\n" + // fully occluded

                "   float d = dot(uvs,uvs);\n" +
                "   float a = 1.0 - d;\n" +
                "   if(a <= 0.0) discard;\n" +
                "   a = 0.05 * sqrt(a) * smoothstep(z0, z1, depth);\n" +
                "   result = vec4((uvs.x*.5+.5)*a, (uvs.y*.5+.5)*a, 0.0, a);\n" +
                "}\n"
    )

    val blendShader = Shader(
        "fluid-blend", listOf(
            Variable(GLSLType.V2F, "positions", VariableMode.ATTR),
        ), "" +
                "void main() {\n" +
                "   uvs = positions;\n" +
                "   gl_Position = vec4(uvs * 2.0 - 1.0, 0.0, 1.0);\n" +
                "}\n", listOf(
            Variable(GLSLType.V2F, "uvs")
        ), listOf(
            Variable(GLSLType.S2D, "colorTex"),
            Variable(GLSLType.S2D, "accuTex"),
            Variable(GLSLType.V3F, "camDirU"),
            Variable(GLSLType.V3F, "camDirV"),
            Variable(GLSLType.SCube, "reflectionMap"),
            Variable(GLSLType.V4F, "result", VariableMode.OUT)
        ), "" +
                "void main() {\n" +
                "   vec4 color = texture(colorTex,uvs);\n" +
                "   vec4 accu = texture(accuTex, uvs);\n" +
                // derive normal for reflections
                "   float density = accu.w;\n" +
                "   float lightFactor = 0.7 * clamp(density * 10.0, 0.0, 1.0);\n" +
                "   vec3 viewDir = cross(camDirU, camDirV);\n" +
                "   vec3 normalDir = normalize(vec3(accu.x/density-0.5, accu.y/density-0.5, -0.5));\n" +
                "   normalDir = camDirU * normalDir.x + camDirV * normalDir.y + viewDir * normalDir.z;\n" +
                "   vec3 reflectDir = reflect(viewDir, normalDir);\n" +
                "   result = color * (1.0 - lightFactor)\n" +
                "       + lightFactor * max(" +
                "           texture(reflectionMap, $cubemapsAreLeftHanded * -reflectDir), " +
                "           texture(reflectionMap, $cubemapsAreLeftHanded * +reflectDir));\n" +
                "}\n"
    )

    val instanceBuffer = StaticBuffer(
        "positions",
        bind(Attribute("particlePositions", 4)), // pos, radius
        particles.size
    )

    override fun executeAction() {
        val width = getIntInput(1)
        val height = getIntInput(2)
        val color = getTextureInput(3) ?: TextureLib.whiteTexture
        val depth = getTextureInput(4) ?: TextureLib.depthTexture

        val accu = FBStack["fluid-accu", width, height, TargetType.UInt16x4, 1, DepthBufferType.NONE]
        val blend = FBStack["fluid-blend", width, height, TargetType.UInt8x4, 1, DepthBufferType.NONE]
        timeRendering("Fluid", timer) {
            useFrame(accu) {
                accu.clearColor(0)

                GFXState.blendMode.use(BlendMode.PURE_ADD) {
                    val camPos = RenderState.cameraPosition

                    // update instance buffer
                    val nio = instanceBuffer.getOrCreateNioBuffer()
                    nio.position(0)
                    nio.limit(particles.size * instanceBuffer.stride)
                    for (i in 0 until particles.size) {
                        nio.putFloat(particles.px[i] - camPos.x.toFloat())
                        nio.putFloat(particles.py[i] - camPos.y.toFloat())
                        nio.putFloat(particles.pz[i] - camPos.z.toFloat())
                        nio.putFloat(particles.radius[i] * 1.5f)
                    }
                    instanceBuffer.cpuSideChanged()
                    instanceBuffer.upload()

                    val shader = accuShader
                    shader.use()

                    shader.m4x4("transform", RenderState.cameraMatrix)
                    shader.v3f("camDirU", Vector3f(1f, 0f, 0f).rotate(RenderState.cameraRotation))
                    shader.v3f("camDirV", Vector3f(0f, 1f, 0f).rotate(RenderState.cameraRotation))
                    shader.v3f("camDirW", RenderState.cameraDirection)

                    depth.bindTrulyNearest(shader, "depthTex")

                    DepthTransforms.bindDepthUniforms(shader)

                    check(instanceBuffer.drawLength > 0)
                    flat01.drawInstanced(shader, instanceBuffer)
                }
            }

            useFrame(blend) {

                val shader = blendShader
                shader.use()

                shader.v3f("camDirU", Vector3f(1f, 0f, 0f).rotate(RenderState.cameraRotation))
                shader.v3f("camDirV", Vector3f(0f, 1f, 0f).rotate(RenderState.cameraRotation))

                color.bindTrulyNearest(shader, "colorTex")
                accu.getTexture0().bindTrulyNearest(shader, "accuTex")

                pipeline.bakedSkybox!!.getTexture0()
                    .bindTrulyLinear(shader, "reflectionMap")

                flat01.draw(shader)
            }
        }

        setOutput(1, Texture.texture(blend, 0))
        setOutput(2, Texture.texture(accu, 0))
    }
}

fun main() {

    val helper = FluidSimulationTests()
    helper.bounds.scale(2f)

    val light = helper.createFluidParticles(1000, density = 1.0f)
    val medium = helper.createFluidParticles(1000, density = 2.0f)
    val heavy = helper.createFluidParticles(1000, density = 3.0f)

    val particles = mergeParticles(light, medium, heavy)
    val solver = helper.createFluidSolver(particles)

    val graph = QuickPipeline()
        .then(BoxCullingNode())
        .then1(RenderDeferredNode(), opaqueNodeSettings)
        .then(RenderLightsNode())
        .then(SSAONode())
        .then(CombineLightsNode())
        .then(SSRNode())
        .then(FluidDensityRenderNode(particles))
        .thenBloomAndExposure()
        .then(GizmoNode())
        .then(FXAANode())
        .finish()

    val renderMode = RenderMode("Fluid", graph)

    val scene = Entity()
        .add(ParticlePhysics(solver, 1f / 60f))

    Entity("Occluder", scene)
        .add(MeshComponent(DefaultAssets.uvSphere, DefaultAssets.steelMaterial))
        .setPosition(0.0, 1.5, 0.0)
        .setScale(3f)
    testSceneWithUI("FluidDensityTest", scene, renderMode)
}