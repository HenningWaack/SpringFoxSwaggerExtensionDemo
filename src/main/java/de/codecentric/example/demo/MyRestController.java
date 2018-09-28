package de.codecentric.example.demo;

import de.codecentric.example.demo.documentation.ApiRoleAccessNotes;
import io.swagger.annotations.*;
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
    @ApiOperation(value = "Get details of a user with the given username", authorizations={@Authorization(value = "BasicAuth")})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Details about the given user"),
            @ApiResponse(code = 401, message = "Cannot authenticate"),
            @ApiResponse(code = 403, message = "Not authorized to get details about the given user")
    })
    @ApiRoleAccessNotes
    public UserDTO getExampleData(@Valid
                                  @ApiParam("Non-empty username") @PathVariable(name = "username", required = true) String username) {
        logger.debug("Getting user with username '%'", username);
        return new UserDTO(username, username + "@test.com", "****", "Test", "user", "My Adress");
    }

    @PostMapping(value = "/user", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @ApiOperation(value = "Create a new user or update an existing user (based on username)", authorizations={@Authorization(value = "BasicAuth")})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "User was successfully updated"),
            @ApiResponse(code = 200, message = "User was successfully created"),
            @ApiResponse(code = 401, message = "Cannot authenticate"),
            @ApiResponse(code = 403, message = "Not authorized to create or update users")
    })
    @ApiRoleAccessNotes
    public void createUser(@Valid @RequestBody UserDTO userDTO) {
        logger.debug("Creating user with username '%'", userDTO.getUsername());
    }
}
