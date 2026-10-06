package ci.devsphere.civmarketplace.util

object LegalContent {
    val TERMS_AND_CONDITIONS = """
        CONDITION GÉNÉRALES D'UTILISATION (CGU)
        Dernière mise à jour : 11 Septembre 2026
        
        1. PRÉSENTATION DE L'APPLICATION
        CIV Marketplace est une plateforme de mise en relation entre vendeurs et acheteurs locaux en Côte d'Ivoire. L'application facilite la découverte de produits et la communication par messagerie instantanée.
        
        2. RÔLES DES UTILISATEURS
        - Acheteur : Peut parcourir, rechercher et commander des produits.
        - Vendeur : Peut créer une boutique et lister des produits.
        
        3. ENGAGEMENTS DU VENDEUR
        Le vendeur s'engage à fournir des informations exactes sur ses produits et sa localisation. Les produits illégaux ou contrefaits sont strictement interdits.
        
        4. MESSAGERIE ET COMPORTEMENT
        Tout comportement abusif, harcèlement ou tentative d'arnaque sur la messagerie entraînera le bannissement immédiat du compte.
        
        5. RESPONSABILITÉ
        CIV Marketplace agit comme intermédiaire technique. Nous ne sommes pas responsables des litiges directs entre acheteurs et vendeurs concernant la qualité des produits ou la livraison.
    """.trimIndent()

    val PRIVACY_POLICY = """
        POLITIQUE DE CONFIDENTIALITÉ
        Dernière mise à jour : 11 Septembre 2026
        
        1. DONNÉES COLLECTÉES
        Nous collectons les informations suivantes pour le bon fonctionnement de l'app :
        - Nom et Prénom
        - Adresse email et téléphone
        - Localisation (Ville, Commune, Quartier) pour le filtrage local.
        - Token FCM pour l'envoi des notifications push.
        
        2. UTILISATION DES DONNÉES
        Vos données servent uniquement à :
        - Créer et sécuriser votre compte.
        - Permettre aux vendeurs de vous contacter pour vos commandes.
        - Afficher les produits proches de chez vous.
        
        3. PARTAGE DES DONNÉES
        Vos coordonnées ne sont partagées qu'avec le vendeur auprès duquel vous passez commande. Nous ne vendons jamais vos données à des tiers.
        
        4. SÉCURITÉ
        Vos mots de passe sont cryptés. Nous utilisons des services sécurisés (Firebase, Pusher) pour la transmission des données en temps réel.
        
        5. VOS DROITS
        Vous pouvez modifier vos informations ou supprimer votre compte à tout moment depuis les paramètres de l'application.
    """.trimIndent()
}

