package abag.xmltosolr;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.jsoup.Jsoup;


import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class PostService {

    final private String solrUrl = "http://localhost:8983/solr";
    final private int batchSize = 50;
    final private String fileLocation = "/media/sf_Downloads/coffee.stackexchange.com/Posts.xml";


    private SolrClient solrClient;

    public PostService() {
        solrClient = new HttpSolrClient.Builder(solrUrl).build();
    }

    public void saveXmlData() throws IOException, SolrServerException {

        int rowNumber = 0;

        try (LineIterator lineIterator = FileUtils.lineIterator(new File(fileLocation))) {

            Instant saveStart = Instant.now();

            List<Post> posts = new ArrayList<>();

            while (lineIterator.hasNext()) {
                String line = lineIterator.nextLine();
                if (line.contains("<row")) {

                    Post post = convertXmlLineToPost(line);
                    posts.add(post);

                    rowNumber++;

                    if (rowNumber % batchSize == 0) {
                        solrClient.addBeans("post", posts);
                        solrClient.commit("post");
                        posts.clear();
                    }

                    if (rowNumber % 100_000 == 0) {
                        System.out.println(rowNumber + " / 10 000 000 rows inserted");
                    }

                    if (rowNumber == 10_000_000) {
                        break;
                    }
                }
            }
            Instant saveEnd = Instant.now();

            long saveTime = Duration.between(saveStart, saveEnd).toMillis();

            System.out.println("Save time: " + saveTime + "ms");

        }
    }

    private Post convertXmlLineToPost(String line) {
        Post post = new Post();

        post.setPostId(Integer.parseInt(extractStringField("Id", line)));

        if (line.contains("PostTypeId=\"")) {
            post.setPostType(Integer.parseInt(extractStringField("PostTypeId=\"", line)));
        }

        if (line.contains("Score=\"")) {
            post.setScore(Integer.parseInt(extractStringField("Score", line)));
        }

        if (line.contains("ViewCount=\"")) {
            post.setViewCount(Integer.parseInt(extractStringField("ViewCount=\"", line)));
        }

        if (line.contains("CreationDate=\"")) {
            String date = extractStringField("CreationDate=\"", line);
            int year = Integer.parseInt(date.substring(0, 4));
            post.setYear(year);
        }

        if (line.contains("Body=\"")) {
            String rawBody = extractStringField("Body=\"", line);
            String bodyText = removeHTMLFromString(rawBody);
            post.setBody(bodyText);
        }

        if (line.contains("Title=\"")) {
            String rawTitle = extractStringField("Title=\"", line);
            String titleText = removeHTMLFromString(rawTitle);
            post.setTitle(titleText);
        }

        return post;
    }

    private String extractStringField(String fieldName, String line) {
        int beginFieldIndex = line.indexOf(fieldName);
        int beginIndex = line.indexOf("\"", beginFieldIndex) + 1;
        int endIndex = line.indexOf("\"", beginIndex);
        return line.substring(beginIndex, endIndex);
    }

    private String removeHTMLFromString(String input) {
        String replace = input.replace("&amp;", "");
        String html = StringEscapeUtils.unescapeHtml4(replace);
        return Jsoup.parse(html).text();
    }

}
