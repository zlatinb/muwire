package com.muwire.core.hostcache

import net.i2p.data.Destination

class CacheServers {

    private static final int TO_GIVE = 3
    private static Set<Destination> CACHES = [
            // zlatinb
            new Destination("Wddh2E6FyyXBF7SvUYHKdN-vjf3~N6uqQWNeBDTM0P33YjiQCOsyedrjmDZmWFrXUJfJLWnCb5bnKezfk4uDaMyj~uvDG~yvLVcFgcPWSUd7BfGgym-zqcG1q1DcM8vfun-US7YamBlmtC6MZ2j-~Igqzmgshita8aLPCfNAA6S6e2UMjjtG7QIXlxpMec75dkHdJlVWbzrk9z8Qgru3YIk0UztYgEwDNBbm9wInsbHhr3HtAfa02QcgRVqRN2PnQXuqUJs7R7~09FZPEviiIcUpkY3FeyLlX1sgQFBeGeA96blaPvZNGd6KnNdgfLgMebx5SSxC-N4KZMSMBz5cgonQF3~m2HHFRSI85zqZNG5X9bJN85t80ltiv1W1es8ZnQW4es11r7MrvJNXz5bmSH641yJIvS6qI8OJJNpFVBIQSXLD-96TayrLQPaYw~uNZ-eXaE6G5dYhiuN8xHsFI1QkdaUaVZnvDGfsRbpS5GtpUbBDbyLkdPurG0i7dN1wAAAA"),
            // sNL
            new Destination("JC63wJNOqSJmymkj4~UJWywBTvDGikKMoYP0HX2Wz9c5l3otXSkwnxWAFL4cKr~Ygh3BNNi2t93vuLIiI1W8AsE42kR~PwRx~Y-WvIHXR6KUejRmOp-n8WidtjKg9k4aDy428uSOedqXDxys5mpoeQXwDsv1CoPTTwnmb1GWFy~oTGIsCguCl~aJWGnqiKarPO3GJQ~ev-NbvAQzUfC3HeP1e6pdI5CGGjExahTCID5UjpJw8GaDXWlGmYWWH303Xu4x-vAHQy1dJLsOBCn8dZravsn5BKJk~j0POUon45CCx-~NYtaPe0Itt9cMdD2ciC76Rep1D0X0sm1SjlSs8sZ52KmF3oaLZ6OzgI9QLMIyBUrfi41sK5I0qTuUVBAkvW1xr~L-20dYJ9TrbOaOb2-vDIfKaxVi6xQOuhgQDiSBhd3qv2m0xGu-BM9DQYfNA0FdMjnZmqjmji9RMavzQSsVFIbQGLbrLepiEFlb7TseCK5UtRp8TxnG7L4gbYevBQAEAAcAAA=="),
            // dark_trion
            new Destination("Gec9L29FVcQvYDgpcYuEYdltJn06PPoOWAcAM8Af-gDm~ehlrJcwlLXXs0hidq~yP2A0X7QcDi6i6shAfuEofTchxGJl8LRNqj9lio7WnB7cIixXWL~uCkD7Np5LMX0~akNX34oOb9RcBYVT2U5rFGJmJ7OtBv~IBkGeLhsMrqaCjahd0jdBO~QJ-t82ZKZhh044d24~JEfF9zSJxdBoCdAcXzryGNy7sYtFVDFsPKJudAxSW-UsSQiGw2~k-TxyF0r-iAt1IdzfNu8Lu0WPqLdhDYJWcPldx2PR5uJorI~zo~z3I5RX3NwzarlbD4nEP5s65ahPSfVCEkzmaJUBgP8DvBqlFaX89K4nGRYc7jkEjJ8cX4L6YPXUpTPWcfKkW259WdQY3YFh6x7rzijrGZewpczOLCrt-bZRYgDrUibmZxKZmNhy~lQu4gYVVjkz1i4tL~DWlhIc4y0x2vItwkYLArPPi~ejTnt-~Lhb7oPMXRcWa3UrwGKpFvGZY4NXBQAEAAcAAA==")
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
