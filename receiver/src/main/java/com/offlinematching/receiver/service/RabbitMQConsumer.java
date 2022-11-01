package com.offlinematching.receiver.service;

import java.io.IOException;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import com.offlinematching.receiver.utils.*;
import com.rabbitmq.client.Channel;

import io.github.cdimascio.dotenv.Dotenv;
@EnableRabbit
@Service
public class RabbitMQConsumer {


    
    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitMQConsumer.class);
    public Counter counter;
 
    @RabbitListener(queues = { "${rabbitmq.queue.name}" })
    public void consume(String message,  Channel channel,  @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws ClassNotFoundException, SQLException, IOException {
        channel.basicAck(tag, false);
        counter = new Counter();
        LOGGER.info(String.format("Received message -> %s", message));
        String[] data = message.split(",");
        String customerNumber = data[0];
        String token = data[1];
        Dotenv dotenv = Dotenv.load();
        Dotenv queries = Dotenv.configure().filename(".queries")
                .load();
        
        Map<String, byte[]> customerFingers = new HashMap<>();
        List<Thread> threadList = new ArrayList<>();
        Connection con;
        Connection logCon;
        Class.forName("oracle.jdbc.OracleDriver");
        con = DriverManager.getConnection(
                dotenv.get("db_url"), dotenv.get("db_user"), dotenv.get("db_password"));
        Class.forName("org.postgresql.Driver");
        logCon = DriverManager.getConnection(dotenv.get("log_db_url"), dotenv.get("log_db_user"),
                dotenv.get("log_db_password"));
        Statement stmt = con.createStatement();
        ResultSet rs = stmt.executeQuery(queries.get("select_customer_count"));
        rs.next();
        // int records = rs.getInt(1);
        int records = 100000;

        Statement fingerStatement = con.createStatement();
        ResultSet fingers = fingerStatement.executeQuery(queries.get("specific_customer") + customerNumber);
        while (fingers.next()) {
            customerFingers.put("RTHUMB", getByteDataFromBlob(fingers.getBlob("RTHUMB")));
            customerFingers.put("RINDEX", getByteDataFromBlob(fingers.getBlob("RINDEX")));
            customerFingers.put("RMIDDLE", getByteDataFromBlob(fingers.getBlob("RMIDDLE")));
            customerFingers.put("RRING", getByteDataFromBlob(fingers.getBlob("RRING")));
            customerFingers.put("RLITTLE", getByteDataFromBlob(fingers.getBlob("RLITTLE")));
            customerFingers.put("LTHUMB", getByteDataFromBlob(fingers.getBlob("LTHUMB")));
            customerFingers.put("LINDEX", getByteDataFromBlob(fingers.getBlob("LINDEX")));
            customerFingers.put("LMIDDLE", getByteDataFromBlob(fingers.getBlob("LMIDDLE")));
            customerFingers.put("LRING", getByteDataFromBlob(fingers.getBlob("LRING")));
            customerFingers.put("LLITTLE", getByteDataFromBlob(fingers.getBlob("LLITTLE")));

        }

        int threads = Integer.parseInt(dotenv.get("number_of_threads"));
        int startIndex = 0;
        int chunk = (int) Math.floor(records / threads);

        for (int i = 0; i < threads; i++) {
            if (i > 0 && (i == (threads - 1))) {
                chunk += records % threads;
            }
            MatchingThread matchingThread = new MatchingThread(counter, token, customerNumber, con, logCon, customerFingers,
                    startIndex,
                    chunk);
            startIndex += chunk;
            Thread thread = new Thread(matchingThread);
            threadList.add(thread);
        }

        Long startTime = System.currentTimeMillis();
        for (int i = 0; i < threadList.size(); i++) {
            threadList.get(i).start();
        }
        try {
            for (int i = 0; i < threadList.size(); i++) {
                threadList.get(i).join();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        System.out.println("Time Taken: " + (System.currentTimeMillis() - startTime));
        PreparedStatement logStmt = logCon.prepareStatement(queries.get("job_update"));
        logStmt.setString(1, token);
        logStmt.execute();
        logStmt.close();
        logCon.close();

    }

    public byte[] getByteDataFromBlob(Blob blob) {
        if (blob != null) {
            try {
                return blob.getBytes(1, (int) blob.length());
            } catch (SQLException ex) {
                System.out.println(ex);
            }
        }
        return null;
    }

    

}