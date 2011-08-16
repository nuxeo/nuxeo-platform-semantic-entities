package org.nuxeo.ecm.platform.semanticentities.service;

import java.util.Calendar;
import java.util.List;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;

public class StanbolEntityHubSourceTest extends RemoteEntityServiceTest {

    @Override
    protected void deployRemoteEntityServiceOverride() throws Exception {
        // deploy off-line mock DBpedia source to override the default source
        // that needs an internet connection: comment the following contrib to
        // test again the real Stanbol server
        deployContrib("org.nuxeo.ecm.platform.semanticentities.core.tests",
                "OSGI-INF/test-semantic-entities-stanbol-entity-contrib.xml");
    }

    @Override
    @SuppressWarnings("unchecked")
    public void testDerefenceRemoteEntity() throws Exception {
        DocumentModel barackDoc = session.createDocumentModel("Person");
        service.dereferenceInto(barackDoc, DBPEDIA_BARACK_OBAMA_URI, true);

        // the title and birth date are fetched from the remote entity
        // description
        assertEquals("Barack Obama", barackDoc.getTitle());

        String summary = barackDoc.getProperty("entity:summary").getValue(
                String.class);
        // TODO: the default entityhub dbpedia index does not store the
        // summaries
        assertNull(summary);

        List<String> altnames = barackDoc.getProperty("entity:altnames").getValue(
                List.class);
        // TODO: the default entityhub dbpedia index does not store many
        // languages for the labels
        assertEquals(1, altnames.size());
        // Western spelling:
        assertTrue(altnames.contains("Barack Obama"));

        Calendar birthDate = barackDoc.getProperty("person:birthDate").getValue(
                Calendar.class);
        assertNotNull(birthDate);
        assertEquals("Fri Aug 04 00:00:00 CET 1961",
                birthDate.getTime().toString());

        Blob depiction = barackDoc.getProperty("entity:depiction").getValue(
                Blob.class);
        assertNotNull(depiction);
        assertEquals("Official_portrait_of_Barack_Obama.jpg",
                depiction.getFilename());
        assertEquals(14748, depiction.getLength());

        List<String> sameas = barackDoc.getProperty("entity:sameas").getValue(
                List.class);
        assertTrue(sameas.contains(DBPEDIA_BARACK_OBAMA_URI.toString()));

        // check that further dereferencing with override == false does not
        // erase local changes
        barackDoc.setPropertyValue("dc:title", "B. Obama");
        barackDoc.setPropertyValue("person:birthDate", null);

        service.dereferenceInto(barackDoc, DBPEDIA_BARACK_OBAMA_URI, false);

        assertEquals("B. Obama", barackDoc.getTitle());
        birthDate = barackDoc.getProperty("person:birthDate").getValue(
                Calendar.class);
        assertEquals("Fri Aug 04 00:00:00 CET 1961",
                birthDate.getTime().toString());

        // existing names are not re-added
        altnames = barackDoc.getProperty("entity:altnames").getValue(List.class);
        assertEquals(1, altnames.size());

        // later dereferencing with override == true does not preserve local
        // changes
        service.dereferenceInto(barackDoc, DBPEDIA_BARACK_OBAMA_URI, true);
        assertEquals("Barack Obama", barackDoc.getTitle());
    }

}
