package com.siran.itemExchange.resource;

import com.siran.itemExchange.dto.ApiError;
import com.siran.itemExchange.dto.UserInput;
import com.siran.itemExchange.dto.UserLoginInput;
import com.siran.itemExchange.dto.UserProfileInput;
import com.siran.itemExchange.dto.UserResponse;
import com.siran.itemExchange.dataObjects.Users;
import com.siran.itemExchange.dataRepositories.UsersRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.Optional;

@Path("/users")
public class UserResource {

    @Autowired
    private UsersRepository usersRepository;

    /**
     * Sign in.
     *
     * Passwords are stored and compared in plain text, which is a deliberate choice for
     * this project and not safe for anything reachable off localhost. Swapping in BCrypt
     * later touches exactly two places: the hash on registration below, and the compare
     * here.
     */
    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response login(@NotNull @Valid UserLoginInput input) {
        Optional<Users> maybeUser = usersRepository.findByUsername(input.getUsername());

        // Same response whether the username is unknown or the password is wrong, so the
        // endpoint can't be used to discover which usernames exist.
        if (maybeUser.isEmpty() || !maybeUser.get().getPassword().equals(input.getPassword())) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(new ApiError("Invalid username or password"))
                    .build();
        }

        return Response.ok(UserResponse.from(maybeUser.get())).build();
    }

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
    public Response updateUser(@PathParam("id") Integer id, @NotNull @Valid UserInput input) {
        Optional<Users> maybeUser = usersRepository.findById(id);
        if (maybeUser.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        Users user = maybeUser.get();
        apply(input, user);
        user.setUpdatedAt(new Date());
        usersRepository.save(user);
        return Response.ok(UserResponse.from(user)).build();
    }

    /**
     * Edit the contact details on a profile. Separate from updateUser because that one
     * needs a username and password, and the client never receives the password back.
     */
    @PUT
    @Path("/profile/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateProfile(@PathParam("id") Integer id, @NotNull @Valid UserProfileInput input) {
        Optional<Users> maybeUser = usersRepository.findById(id);
        if (maybeUser.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        String email = input.getEmail().trim().toLowerCase();
        if (usersRepository.existsByEmailAndIdNot(email, id)) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(new ApiError("That email is already registered"))
                    .build();
        }

        Users user = maybeUser.get();
        user.setEmail(email);
        user.setPhone(input.getPhone());
        user.setCollege(input.getCollege());
        user.setUpdatedAt(new Date());
        usersRepository.save(user);
        return Response.ok(UserResponse.from(user)).build();
    }

    @POST
    @Path("/add")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addNewUserJson (@NotNull @Valid UserInput userInput) {
        // Checked up front so the client gets a message it can show, rather than a
        // unique-constraint failure from MySQL.
        if (usersRepository.existsByUsername(userInput.getUsername().trim())) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(new ApiError("That username is already taken"))
                    .build();
        }
        if (usersRepository.existsByEmail(userInput.getEmail().trim().toLowerCase())) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(new ApiError("That email is already registered"))
                    .build();
        }

        Users user = new Users();
        apply(userInput, user);
        user.setCreatedAt(new Date());
        user.setUpdatedAt(new Date());
        usersRepository.save(user);
        return Response.status(Response.Status.CREATED).entity(UserResponse.from(user)).build();
    }

    private static void apply(UserInput in, Users user) {
        user.setUsername(in.getUsername().trim());
        user.setPassword(in.getPassword());
        user.setEmail(in.getEmail().trim().toLowerCase());
        user.setPhone(in.getPhone());
        user.setCollege(in.getCollege());
    }
}
