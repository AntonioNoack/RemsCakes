package me.anno.chemistry

import me.anno.utils.Color.black

// todo describe all possible chemical reactions within the product
//  mixing is handled by physics
//  and we need a reaction detection logic
// todo reactions, that take time, only slowly convert things
// todo do we want "burnt" variants?
//  each component has their own burning-temperature,
//  but creating a burnt-state for all is exhausting,
//  but it would be useful to say what was burned in the food
// todo airial and contact heat transfer

enum class Compound(
    val color: Int,
    val meltingTemp: Float,
    val evaporationTemp: Float,
    val burningTemp: Float,
    val roughness: Float,
    val type: SolidType
) {

    WATER(0xffffff, 0f, 100f, 1e9f, 0f, SolidType.CRYSTAL),
    FLOUR(0xffff99 or black, -100f, 400f, 200f, 1f, SolidType.POWDER),
    YEAST(0xD8C090 or black, 50f, 200f, 120f, 1f, SolidType.POWDER),
    BAKING_SODA(0xFFFFFF or black, 1e9f, 400f, 1e9f, 1f, SolidType.POWDER),
    BAKING_POWDER(0xF8F8F8 or black, 1e9f, 400f, 1e9f, 1f, SolidType.POWDER),

    SOFT_DOUGH(0xffaa77 or black, 0f, 400f, 200f, 1f, SolidType.SOFT_COOKIE), // after baking
    BRITTLE_DOUGH(0xffaa77 or black, 0f, 400f, 200f, 1f, SolidType.BRITTLE_COOKIE), // after baking

    STRAWBERRY(0xff3333 or black, 0f, 250f, 200f, 0.3f, SolidType.FIBROUS),
    STRAWBERRY_JUICE(0xff3333 or black, 0f, 250f, 200f, 0.5f, SolidType.CRYSTAL),

    BANANA(0xF3E36A or black, 0f, 180f, 160f, 0.4f, SolidType.FIBROUS),
    BANANA_JUICE(0xF3E36A or black, 0f, 180f, 160f, 0.4f, SolidType.CRYSTAL),

    BLUEBERRY(0x3A3A88 or black, 0f, 160f, 180f, 0.3f, SolidType.FIBROUS),
    BLUEBERRY_JUICE(0x3A3A88 or black, 0f, 160f, 180f, 0.3f, SolidType.CRYSTAL),

    LEMON(0xFFF799 or black, 0f, 100f, 150f, 0f, SolidType.FIBROUS),
    LEMON_JUICE(0xFFF799 or black, 0f, 100f, 150f, 0f, SolidType.CRYSTAL),

    APPLE(0xD8C070 or black, 0f, 170f, 180f, 0.5f, SolidType.FIBROUS),
    APPLE_JUICE(0xD8C070 or black, 0f, 170f, 180f, 0.5f, SolidType.CRYSTAL),

    SUGAR(0xffffff or black, 1e9f, 600f, 150f, 1f, SolidType.CRYSTAL),
    CARAMEL(0xcc8866 or black, 1e9f, 600f, 200f, 0.2f, SolidType.CRYSTAL),

    BROWN_SUGAR(0x8B5A2B or black, 1e9f, 500f, 160f, 0.8f, SolidType.CRYSTAL),
    HONEY(0xDFAF2C or black, -20f, 200f, 170f, 0.02f, SolidType.GEL),
    MARSHMALLOW(0xFFF8F0 or black, 35f, 180f, 150f, 0.05f, SolidType.FOAM),

    CACAO(0x4A3631 or black, 50f, 300f, 70f, 1f, SolidType.POWDER), // loses taste at 70, burns at 200
    CACAO_BUTTER(0x434343 or black, 34f, 500f, 230f, 0.1f, SolidType.SQUEEZABLE),
    WHITE_CHOCOLATE(0xBDB39A or black, 28f, 300f, 46f, 0.2f, SolidType.HARD),
    MILK_CHOCOLATE(0x956545 or black, 30f, 300f, 46f, 0.2f, SolidType.HARD),
    DARK_CHOCOLATE(0x382818 or black, 32f, 300f, 50f, 0.2f, SolidType.HARD),

    MILK(0xffffff, 0f, 100f, 70f, 0f, SolidType.CRYSTAL),

    BUTTER(0xF7D86A or black, 32f, 250f, 180f, 0.05f, SolidType.SQUEEZABLE),

    EGG_WHITE(0xF8F8F8 or black, 60f, 150f, 180f, 0.1f, SolidType.GEL),
    EGG_YOLK(0xFFD84A or black, 65f, 150f, 180f, 0.15f, SolidType.GEL),
    WHOLE_EGG(0xFFE27A or black, 64f, 150f, 180f, 0.12f, SolidType.GEL),

    CREAM(0xFFF4DD or black, 28f, 120f, 170f, 0.05f, SolidType.SQUEEZABLE),
    WHIPPED_CREAM(0xFFFFFF or black, 26f, 120f, 160f, 0.02f, SolidType.FOAM),
    CUSTARD(0xF4D27A or black, 70f, 150f, 180f, 0.2f, SolidType.GEL), // pudding

}