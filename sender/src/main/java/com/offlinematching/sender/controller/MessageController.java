package com.offlinematching.sender.controller;

import java.sql.Connection;
import java.sql.DriverManager;
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

    // http://localhost:8080/api/v1/publish?message=hello
    @GetMapping("/publish")
    public ResponseEntity<Response> sendMessage(@RequestParam("message") String message) throws SQLException, ClassNotFoundException{
        
        Response res = new Response();
        Class.forName("oracle.jdbc.OracleDriver");
        Connection con = DriverManager.getConnection(
                dotenv.get("db_url"), dotenv.get("db_user"), dotenv.get("db_password"));

        java.sql.Statement stmt = con.createStatement();
        ResultSet rs = stmt.executeQuery("select count(*) from fp_enroll where cust_no=" + message);
        rs.next();
        int records = rs.getInt(1);
       
        if (records >= 1)
        {
            res.setCustomerNumber(message);
            String token = (String) UUID.randomUUID().toString();
            message += "," + token;
        res.setMessage("Successful");
        res.setToken(token);
        producer.sendMessage(message);
        return new ResponseEntity<>(res, HttpStatus.OK);
        }else{
            res.setCustomerNumber(message);
            res.setMessage("Customer does not exist!");
            res.setToken("N/A");
            return new ResponseEntity<>(res, HttpStatus.BAD_REQUEST);
        }
       
        
    }
}