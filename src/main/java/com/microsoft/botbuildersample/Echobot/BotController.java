package com.microsoft.botbuildersample.Echobot;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.microsoft.aad.adal4j.AuthenticationException;
import com.microsoft.bot.connector.authentication.CredentialProvider;
import com.microsoft.bot.connector.authentication.CredentialProviderImpl;
import com.microsoft.bot.connector.authentication.JwtTokenValidation;
import com.microsoft.bot.connector.authentication.MicrosoftAppCredentials;
import com.microsoft.bot.connector.implementation.ConnectorClientImpl;
import com.microsoft.bot.schema.models.Activity;
import com.microsoft.bot.schema.models.ActivityTypes;
import com.microsoft.bot.schema.models.ResourceResponse;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

@RestController
public class BotController {
	private static final Logger LOGGER = Logger.getLogger( BotController.class.getName() );
    private static String appId = "<-- app id -->";
    private static String appPassword = "<-- app password -->";
	private CredentialProvider _credentialProvider = new CredentialProviderImpl(appId, appPassword);
	private MicrosoftAppCredentials _credentials;
	private FileHandler _filehandler;
	
	public BotController()
	{
		_credentials = new MicrosoftAppCredentials(appId, appPassword);
		_filehandler = null;
        try {

            // Log to appropriate place in Azure
            _filehandler = new FileHandler("D:/home/LogFiles/Echobot.log");
            LOGGER.addHandler(_filehandler);
            SimpleFormatter formatter = new SimpleFormatter();
            _filehandler.setFormatter(formatter);

            // the following statement is used to log any messages
            LOGGER.info("My first log");

        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
	}

	@GetMapping("/hello")
    public String hello()
    {
        LOGGER.log(Level.WARNING, "Hello received!");
        _filehandler.flush();
        return "Hello there";
    }
    
    @PostMapping("/api/messages")
    public ResponseEntity<Object> incoming(@RequestBody Activity activity, @RequestHeader(value="Authorization") String authHeader) {
        LOGGER.log(Level.WARNING, "Received a new message.");
        _filehandler.flush();
    	try {

            JwtTokenValidation.authenticateRequest(activity, authHeader, _credentialProvider);

            if (activity.type().equals(ActivityTypes.MESSAGE)) {
                LOGGER.log(Level.INFO, "Activity type of Message received..");

                // reply activity with the same text
                ConnectorClientImpl connector = new ConnectorClientImpl(activity.serviceUrl(), _credentials);
                ResourceResponse response = connector.conversations().sendToConversation(activity.conversation().id(),
                        new Activity()
                                .withType(ActivityTypes.MESSAGE)
                                .withText("Echo: " + activity.text())
                                .withRecipient(activity.from())
                                .withFrom(activity.recipient())
                );
                LOGGER.log(Level.INFO, "After send..");
                _filehandler.flush();

            }
        } catch (AuthenticationException ex) {
            LOGGER.log(Level.WARNING, "Auth failed!", ex);
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Execution failed", ex);
        }
    	
        // send ack to user activity
        return new ResponseEntity<>(HttpStatus.ACCEPTED);

    }    
}
