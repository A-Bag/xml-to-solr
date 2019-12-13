package abag.xmltosolr;

import org.apache.solr.client.solrj.SolrServerException;

import java.io.IOException;

public class XmlToSolrApplication {
    public static void main(String[] args) throws IOException, SolrServerException {
        new PostService().saveXmlData();
    }
}
