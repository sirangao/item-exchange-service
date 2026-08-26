package com.siran.itemExchange.resource;

import com.siran.itemExchange.dto.UserInput;
import com.siran.itemExchange.dto.UserResponse;
import com.siran.itemExchange.dataObjects.Users;
import com.siran.itemExchange.dataRepositories.UsersRepository;
import com.siran.itemExchange.dto.UserUpdateInput;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Date;
import java.util.Optional;

@Path("/users")
public class UserResource {

    @Autowired
    private UsersRepository usersRepository;

    @GET
    @Path("/{username}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getByUsername(@PathParam("username") String username){

        Optional<Users> maybeUser = usersRepository.findByUsername(username);

        if (maybeUser.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        Users user = maybeUser.get();
        UserResponse body = UserResponse.from(user);
        return Response.ok(body).build();

//        return usersRepository.findByUsername(username)
//                .map(user -> Response.ok(UserResponse.from(user)).build())
//                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND).build());

        /*
        if: Users findByUsername(String username);

        then:
        Users user = usersRepository.findByUsername(username);
        if (user == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(UserResponse.from(user)).build();
         */
    }

    @GET
    @Path("/id/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getById(@PathParam("id") Integer id){
        Optional<Users> maybeUser = usersRepository.findById(id);

        if (maybeUser.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        Users user = maybeUser.get();
        UserResponse body = UserResponse.from(user);
        return Response.ok(body).build();
    }

    @PUT
    @Path("/updateById/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateUser(@PathParam("id") Integer id, @NotNull @Valid UserUpdateInput input){
        Optional<Users> maybeUser = usersRepository.findById(id);
        if (maybeUser.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        Users user = maybeUser.get();
        user.setEmail(input.getEmail());
        user.setPhone(input.getPhone());
        user.setCollege(input.getCollege());
        user.setUpdatedAt(new Date());

        usersRepository.save(user);
        return Response.ok(UserResponse.from(user)).build();
    }

    @POST
    @Path("/add")
    @Consumes(MediaType.APPLICATION_JSON)
    public String addNewUserJson (@RequestBody @NotNull @Valid UserInput userInput) {
        Users n = new Users();
        n.setUsername(userInput.getUsername());
        n.setPassword(userInput.getPassword());
        n.setEmail(userInput.getEmail());
        usersRepository.save(n);
        return "Saved";
    }

//    @POST
//    @Path("/add")
//    public String addNewUserParam (@QueryParam("username") String username, @QueryParam("password") String password, @QueryParam("email") String email) {
//        Users n = new Users();
//        n.setUsername(username);
//        n.setPassword(password);
//        n.setEmail(email);
//        usersRepository.save(n);
//        return "Saved";
//    }
}
