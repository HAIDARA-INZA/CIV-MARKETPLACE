package ci.devsphere.civmarketplace.data

/**
 * Données de localisation pour la recherche intelligente (Ville / Commune / Quartier)
 * à l'inscription et sur le profil. Liste non exhaustive mais couvre les grandes villes
 * de Côte d'Ivoire et les communes/quartiers principaux d'Abidjan — facile à compléter
 * plus tard en ajoutant des entrées dans les listes ci-dessous.
 */
object LocationData {

    val cities: List<String> = listOf(
        "Abidjan", "Yamoussoukro", "Bouaké", "Daloa", "San-Pédro", "Korhogo",
        "Man", "Divo", "Gagnoa", "Abengourou", "Anyama", "Grand-Bassam",
        "Agboville", "Dabou", "Bondoukou", "Séguéla", "Odienné", "Soubré",
        "Bingerville", "Adzopé"
    )

    /** Communes d'Abidjan (les autres villes n'ont pas de découpage en communes ici). */
    val communesByCity: Map<String, List<String>> = mapOf(
        "Abidjan" to listOf(
            "Abobo", "Adjamé", "Attécoubé", "Cocody", "Koumassi", "Marcory",
            "Plateau", "Port-Bouët", "Treichville", "Yopougon", "Bingerville",
            "Songon", "Anyama"
        )
    )

    /** Quartiers connus par commune (liste indicative, pas exhaustive). */
    val quartersByCommune: Map<String, List<String>> = mapOf(
        "Abobo" to listOf("Abobo Gare", "Abobo Baoulé", "Anonkoua-Kouté", "Avocatier", "Sagbé", "N'Dotré"),
        "Adjamé" to listOf("Adjamé Liberté", "Adjamé 220 Logements", "Bracodi", "Williamsville", "Petit Paris"),
        "Attécoubé" to listOf("Locodjro", "Abobo-Doumé", "Agban", "Santé"),
        "Cocody" to listOf("Angré", "Riviera", "Deux Plateaux", "II Plateaux Vallon", "Danga", "Bonoumin", "M'Badon", "Saint-Jean", "Faya"),
        "Koumassi" to listOf("Koumassi Grand Marché", "Koumassi Remblais", "Koumassi Prodomo", "Sicogi"),
        "Marcory" to listOf("Marcory Résidentiel", "Zone 4", "Biétry", "Anoumabo", "Remblais"),
        "Plateau" to listOf("Plateau Centre", "Indénié", "Vallon", "Cité Administrative"),
        "Port-Bouët" to listOf("Port-Bouët Centre", "Vridi", "Gonzagueville", "Jean Folly", "Aéroport"),
        "Treichville" to listOf("Treichville Centre", "Arras", "Belleville", "Biafra"),
        "Yopougon" to listOf(
            "Yopougon Sideci", "Yopougon Niangon", "Yopougon Selmer", "Yopougon Toits Rouges",
            "Novalim", "Yopougon Maroc", "Yopougon Wassakara", "Yopougon Andokoi", "Yopougon Gesco",
            "Port-Bouët 2", "Yopougon Koweit"
        ),
        "Bingerville" to listOf("Bingerville Centre", "Adjin", "M'Badon Bingerville"),
        "Songon" to listOf("Songon Agban", "Songon Kassemblé"),
        "Anyama" to listOf("Anyama Centre", "Anyama Adjamé", "N'Dotré Anyama")
    )

    /** Suggestions filtrées par préfixe/contenu, insensible à la casse. Vide si la requête l'est. */
    fun searchCities(query: String): List<String> =
        if (query.isBlank()) cities else cities.filter { it.contains(query, ignoreCase = true) }

    fun searchCommunes(city: String, query: String): List<String> {
        val pool = communesByCity[city] ?: emptyList()
        return if (query.isBlank()) pool else pool.filter { it.contains(query, ignoreCase = true) }
    }

    fun searchQuarters(commune: String, query: String): List<String> {
        val pool = quartersByCommune[commune] ?: emptyList()
        return if (query.isBlank()) pool else pool.filter { it.contains(query, ignoreCase = true) }
    }
}

