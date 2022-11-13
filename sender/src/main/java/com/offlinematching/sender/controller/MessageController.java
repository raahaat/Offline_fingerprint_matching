package com.offlinematching.sender.controller;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.offlinematching.sender.payload.Response;
import com.offlinematching.sender.service.RabbitMQProducer;

import io.github.cdimascio.dotenv.Dotenv;

@RestController
@RequestMapping("/api/v1")
public class MessageController {

    private RabbitMQProducer producer;

    public MessageController(RabbitMQProducer producer) {
        this.producer = producer;
    }

    Dotenv dotenv = Dotenv.load();

    @GetMapping("/publish")
    public ResponseEntity<Response> sendMessage(@RequestParam("message") String message)
            throws SQLException, ClassNotFoundException {

        Response res = new Response();
        Dotenv queries = Dotenv.configure().filename(".queries")
                .load();

        Class.forName("oracle.jdbc.OracleDriver");
        Connection con = DriverManager.getConnection(
                dotenv.get("db_url"), dotenv.get("db_user"), dotenv.get("db_password"));

        java.sql.Statement stmt = con.createStatement();
        ResultSet rs = stmt.executeQuery("select count(*) from fp_enroll where cust_no=" + message);
        rs.next();
        int records = rs.getInt(1);

        if (records >= 1) {
            String token = (String) UUID.randomUUID().toString();
            Connection logCon = DriverManager.getConnection(dotenv.get("log_db_url"), dotenv.get("log_db_user"),
                    dotenv.get("log_db_password"));
            PreparedStatement logStmt = logCon.prepareStatement(queries.get("job_logs"));
            logStmt.setString(1, message);
            logStmt.setString(2, token);
            logStmt.setString(3, "PENDING");
            logStmt.execute();
            logStmt.close();
            logCon.close();
            res.setCustomerNumber(message);
            message += "," + token;
            res.setMessage("Successful");
            res.setToken(token);
            producer.sendMessage(message);
            stmt.close();
            con.close();
            return new ResponseEntity<>(res, HttpStatus.OK);
        } else {
            res.setCustomerNumber(message);
            res.setMessage("Customer does not exist!");
            res.setToken("N/A");
            return new ResponseEntity<>(res, HttpStatus.BAD_REQUEST);
        }

    }
}