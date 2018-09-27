package de.codecentric.example.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
public class MyRestController {

    Logger logger = LoggerFactory.getLogger(MyRestController.class);

    @RequestMapping(method = RequestMethod.GET, value = "/user/{username}", produces = APPLICATION_JSON_VALUE)
    public UserDTO getExampleData(@Valid @PathVariable(name = "username", required = true) String username) {
        logger.debug("Getting user with username '%'", username);
        return new UserDTO(username, username + "@test.com", "****", "Test", "user", "My Adress");
    }

    @PostMapping(value = "/user", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public void createUser(@Valid @RequestBody UserDTO userDTO) {
        logger.debug("Creating user with username '%'", userDTO.getUsername());
    }

}
