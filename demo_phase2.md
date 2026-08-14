# Scénario de Démo Phase 2 : TrustAI-Chain - Hallucination Guard & PDF Signé

Cette démo montre comment le système TrustAI-Chain permet d'ingérer un document bancaire réel, d'interroger l'IA, de déclencher l'audit (Hallucination Guard), puis d'exporter le rapport audité et signé cryptographiquement.

## Prérequis
1. Assurez-vous que les containers Docker (`postgres`, `keycloak`, `kafka`) sont lancés.
2. Démarrez les services `gateway-service`, `rag-service` et le `web-client`.
3. Connectez-vous à l'application Angular (`http://localhost:4200`) avec un compte ayant le rôle `admin` ou `analyst` (ex: admin / admin).

---

## Étape 1 : Ingestion du document
1. Allez sur le **Dashboard** (ou dans l'onglet de gestion des documents).
2. Uploadez le fichier joint à ce projet : `Conditions_Tarifaires_2024.txt`.
3. Attendez la confirmation. Le document est maintenant vectorisé et stocké dans PostgreSQL via `pgvector`.

## Étape 2 : Interaction et Déclenchement de l'Audit
1. Allez sur la page **Chat**.
2. Posez la question suivante pour forcer l'IA à utiliser le document :
   > *"Quels sont les frais pour un retrait à l'étranger hors zone SEPA avec la BNI, et quel est le taux du crédit sur 20 ans ?"*
3. L'IA va répondre en se basant sur le texte. Observez le **Badge de Confiance** s'afficher à côté du nom de l'IA.
4. Cliquez sur la flèche sous le badge de confiance pour ouvrir le **Panneau d'Audit**.
   - Vous y verrez les affirmations extraites de la réponse.
   - Pour chaque affirmation, vérifiez le statut (VÉRIFIÉ, INCERTAIN) et les sources (Chunks) exactes d'où l'information a été tirée.

## Étape 3 : Export et Signature Numérique du Rapport
1. À côté du bouton d'audit (sur le message de l'IA), cliquez sur la nouvelle icône **PDF**.
2. Le navigateur télécharge le fichier `guard_report_{id}.pdf`.
3. Ouvrez le PDF. Vous y trouverez la question posée, la réponse de l'IA, le score de confiance global, et le détail de chaque affirmation avec ses sources.
4. *(Sous le capot, le service backend a haché ce PDF avec SHA-256 et l'a signé avec la clé privée RSA contenue dans `keystore.p12`. La signature est enregistrée en base de données).*

## Étape 4 : Visualisation de l'Historique (Nouveau Dashboard)
1. Naviguez vers le **Dashboard**.
2. Cliquez sur le nouvel onglet **"Historique des Rapports"**.
3. Vous verrez le tableau (Angular Material DataTable) affichant :
   - La Date et l'Heure de génération du rapport.
   - Le Score de Confiance (coloré selon la pertinence).
   - L'Utilisateur ayant déclenché la génération.
4. Vous pouvez recliquer sur l'icône **PDF** dans ce tableau pour re-télécharger ce même rapport exact.

---
**FIN DE LA DÉMO**
