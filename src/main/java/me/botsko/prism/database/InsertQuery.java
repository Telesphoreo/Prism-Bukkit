package me.botsko.prism.database;

import me.botsko.prism.actions.Handler;

/**
 * Created for use for the Add5tar MC Minecraft server
 * Created by benjamincharlton on 1/06/2019.
 */
public interface InsertQuery {

    long insertActionIntoDatabase(Handler a);

    void createBatch() throws Exception;

    boolean addInsertiontoBatch(Handler a) throws Exception;

    void processBatch() throws Exception;

}
