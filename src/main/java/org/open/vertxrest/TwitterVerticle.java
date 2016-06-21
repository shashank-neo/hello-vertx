package org.open.vertxrest;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import twitter4j.*;
import twitter4j.conf.ConfigurationBuilder;

import java.util.List;

/**
 * Created by shwet.s under project vertx-rest. <br/>
 * Created on  20/06/16. <br/>
 * Updated on 20/06/16.  <br/>
 * Updated by shwet.s. <br/>
 */
public class TwitterVerticle extends AbstractVerticle {

    private List<String> filters;
    private TwitterStream twitterStream;
    private String streamId;

    public TwitterVerticle(List<String> filters, String streamId) {
        this.filters = filters;
        this.streamId = streamId;
    }

    public void start() {
        ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.setDebugEnabled(true);
        cb.setOAuthConsumerKey("Zet7b9zDyV7KUyHtvcYZLo8ZW");
        cb.setOAuthConsumerSecret("33FO4GDGmhrvjip21Ci48dFP5oKR7hY2ylrUfVhsayCkipX4M1");
        cb.setOAuthAccessToken("35552292-lprSBBBTE11PJi8cIcn4EEIhdUJAa0lcx5vv1VTVy");
        cb.setOAuthAccessTokenSecret("ObXZGnEUwCGKVaPWKqvklMQsjAbz2triodWUz4pOEWw8s");

        twitterStream = new TwitterStreamFactory(cb.build()).getInstance();
        StatusListener listener = new StatusListener() {
            public void onStatus(Status status) {
                System.out.println("Dumping to " + "messageToClient_" + streamId);
                JsonObject tweet = new JsonObject();
                tweet.put("tweet", status.getText()).put("user", status.getUser().getName());
                vertx.eventBus().send("messageToClient_" + streamId, tweet.toString());
                System.out.println(tweet.toString());
            }

            public void onDeletionNotice(StatusDeletionNotice statusDeletionNotice) {
                System.out.println("Got a status deletion notice id:" + statusDeletionNotice.getStatusId());
            }

            public void onTrackLimitationNotice(int numberOfLimitedStatuses) {
                System.out.println("Got track limitation notice:" + numberOfLimitedStatuses);
            }

            public void onScrubGeo(long userId, long upToStatusId) {
                System.out.println("Got scrub_geo event userId:" + userId + " upToStatusId:" + upToStatusId);
            }

            // @Override
            public void onStallWarning(StallWarning stallWarning) {

            }

            public void onException(Exception ex) {
                ex.printStackTrace();
            }
        };

        FilterQuery fq = new FilterQuery();
        String[] keywordsArray = new String[filters.size()];
        for(int i = 0; i< keywordsArray.length; i++) {
            keywordsArray[i] = filters.get(i);
        }
        fq.track(keywordsArray);
        twitterStream.addListener(listener);
        twitterStream.filter(fq);

    }

    public void stop() {
        twitterStream.clearListeners();
        twitterStream.shutdown();
    }
}
