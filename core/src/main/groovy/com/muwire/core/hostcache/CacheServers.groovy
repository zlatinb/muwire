package com.muwire.core.hostcache

import net.i2p.data.Destination

class CacheServers {

    private static final int TO_GIVE = 3
    private static Set<Destination> CACHES = [
            // zlatinb
            new Destination("Wddh2E6FyyXBF7SvUYHKdN-vjf3~N6uqQWNeBDTM0P33YjiQCOsyedrjmDZmWFrXUJfJLWnCb5bnKezfk4uDaMyj~uvDG~yvLVcFgcPWSUd7BfGgym-zqcG1q1DcM8vfun-US7YamBlmtC6MZ2j-~Igqzmgshita8aLPCfNAA6S6e2UMjjtG7QIXlxpMec75dkHdJlVWbzrk9z8Qgru3YIk0UztYgEwDNBbm9wInsbHhr3HtAfa02QcgRVqRN2PnQXuqUJs7R7~09FZPEviiIcUpkY3FeyLlX1sgQFBeGeA96blaPvZNGd6KnNdgfLgMebx5SSxC-N4KZMSMBz5cgonQF3~m2HHFRSI85zqZNG5X9bJN85t80ltiv1W1es8ZnQW4es11r7MrvJNXz5bmSH641yJIvS6qI8OJJNpFVBIQSXLD-96TayrLQPaYw~uNZ-eXaE6G5dYhiuN8xHsFI1QkdaUaVZnvDGfsRbpS5GtpUbBDbyLkdPurG0i7dN1wAAAA"),
            // echelon
            new Destination("2MJTl8gYVPK43iJZJa~-5K1OchgPaPHXpqZmKIiKFvxyy8BlIJzUSrF4mazdta--shFHISfT0PEeI95j1yDyKMpGxatUyjSt3ZnyTfAehQR-H2kYV9FvjHo68uA9X5AaGYHKRYLuWMkihMXygd8ywoLjZtFP0UbKMPggfOZaWmjHF4081XoUXt~7MEAeYSQowndiUx0AH3HxNEiv0N373JJS61OsIXb5ctqVKkwIiX1R0ZxESzpP9Xwp8-T0ou8fsLksygbKyH~3K1CyTHjTS51Ux-U-CjOPH9rtCOjjAaifdyMpK0PxW1fVdoGswFywTz9Q-6DUMsIu5TsPMF0-UO1Wn8vCpVAWbBJAOtKCfBrGzp-E~GCbfCNs5xY19nLobMD5ehjsBdI1lXwGDCQ7kBOwC58uuC3BOoazgrB6IrGskyMTexawtthO9mhuPm91bq4xhNaCYHAe059xg5emnM7jFBVzQgjaZ5lOLn~HqcWofJ7oc0doE6XI6kOo~YncBQAEAAcAAA==")
        ]

    static List<Destination> getCacheServers() {
        List<Destination> allCaches = new ArrayList<>(CACHES)
        Collections.shuffle(allCaches)
        if (allCaches.size() <= TO_GIVE)
            return allCaches
        allCaches[0..TO_GIVE-1]
    }

    static boolean isRegistered(Destination d) {
        return CACHES.contains(d)
    }
}
