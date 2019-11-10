package com.muwire.core.trust

import org.junit.After
import org.junit.Before
import org.junit.Test

import com.muwire.core.Destinations
import com.muwire.core.Persona
import com.muwire.core.Personas

import groovy.json.JsonSlurper
import net.i2p.data.Base64
import net.i2p.data.Destination

class TrustServiceTest {

    TrustService service
    File persistGood, persistBad
    Personas personas = new Personas()

    @Before
    void before() {
        persistGood = new File("good.trust")
        persistBad = new File("bad.trust")
        persistGood.delete()
        persistBad.delete()
        persistGood.deleteOnExit()
        persistBad.deleteOnExit()
        service = new TrustService(persistGood, persistBad, 100)
        service.start()
    }

    @After
    void after() {
        service.stop()
    }

    @Test
    void testEmpty() {
        assert TrustLevel.NEUTRAL == service.getLevel(personas.persona1.destination)
        assert TrustLevel.NEUTRAL == service.getLevel(personas.persona2.destination)
    }

    @Test
    void testOnEvent() {
        service.onTrustEvent new TrustEvent(level: TrustLevel.TRUSTED, persona: personas.persona1)
        service.onTrustEvent new TrustEvent(level: TrustLevel.DISTRUSTED, persona: personas.persona2)

        assert TrustLevel.TRUSTED == service.getLevel(personas.persona1.destination)
        assert TrustLevel.DISTRUSTED == service.getLevel(personas.persona2.destination)
    }

    @Test
    void testPersist() {
        service.onTrustEvent new TrustEvent(level: TrustLevel.TRUSTED, persona: personas.persona1)
        service.onTrustEvent new TrustEvent(level: TrustLevel.DISTRUSTED, persona: personas.persona2)

        Thread.sleep(250)
        JsonSlurper slurper = new JsonSlurper()
        def trusted = new HashSet<>()
        persistGood.eachLine {
            def json = slurper.parseText(it)
            trusted.add(new Persona(new ByteArrayInputStream(Base64.decode(json.persona))))
        }
        def distrusted = new HashSet<>()
        persistBad.eachLine {
            def json = slurper.parseText(it)
            distrusted.add(new Persona(new ByteArrayInputStream(Base64.decode(json.persona))))
        }

        assert trusted.size() == 1
        assert trusted.contains(personas.persona1)
        assert distrusted.size() == 1
        assert distrusted.contains(personas.persona2)
    }

    @Test
    void testLoad() {
        service.stop()
        persistGood.append("${personas.persona1.toBase64()}\n")
        persistBad.append("${personas.persona2.toBase64()}\n")
        service = new TrustService(persistGood, persistBad, 100)
        service.start()
        Thread.sleep(50)

        assert TrustLevel.TRUSTED == service.getLevel(personas.persona1.destination)
        assert TrustLevel.DISTRUSTED == service.getLevel(personas.persona2.destination)
    }
}
