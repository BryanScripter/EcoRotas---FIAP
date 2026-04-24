package com.example.ecorotas.data

// Fatores de CO2 por km (kg/km). Carro e ônibus têm valor; bike zero.
object Emissions {
    const val CAR_FACTOR = 0.192
    const val BUS_FACTOR = 0.105
    const val BIKE_FACTOR = 0.0

    enum class TransportMode(val factor: Double, val label: String) {
        CAR(CAR_FACTOR, "Carro"),
        BUS(BUS_FACTOR, "Ônibus"),
        BIKE(BIKE_FACTOR, "Bicicleta")
    }

    fun calculateEmission(distance: Double, mode: TransportMode): Double =
        (distance * mode.factor * 1000).toLong() / 1000.0

    fun getBestOption(distance: Double): TransportMode =
        TransportMode.entries.minByOrNull { calculateEmission(distance, it) } ?: TransportMode.CAR

    fun calculateSavings(distance: Double, mode: TransportMode): Double {
        val carEmission = calculateEmission(distance, TransportMode.CAR)
        val modeEmission = calculateEmission(distance, mode)
        return ((carEmission - modeEmission) * 1000).toLong() / 1000.0
    }

    val ECO_PHRASES = listOf(
        "Cada quilômetro sustentável conta para um planeta melhor!",
        "Você está fazendo a diferença! Continue escolhendo rotas verdes.",
        "Pequenas ações geram grandes mudanças. Parabéns pela escolha!",
        "O planeta agradece suas decisões sustentáveis!",
        "Mobilidade consciente é o caminho para um futuro mais verde.",
        "Sua pegada de carbono está diminuindo. Continue assim!",
        "Cada viagem sustentável inspira uma nova geração.",
        "Juntos podemos reduzir as emissões de carbono!"
    )

    fun getRandomEcoPhrase(): String = ECO_PHRASES.random()
}
