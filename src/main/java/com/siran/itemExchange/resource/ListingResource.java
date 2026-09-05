package com.siran.itemExchange.resource;

import com.siran.itemExchange.dataObjects.Listings;
import com.siran.itemExchange.dataObjects.Users;
import com.siran.itemExchange.dataRepositories.CategoriesRepository;
import com.siran.itemExchange.dataRepositories.ListingsRepository;
import com.siran.itemExchange.dataRepositories.UsersRepository;
import com.siran.itemExchange.dto.ListingInput;
import com.siran.itemExchange.dto.ListingResponse;
import com.siran.itemExchange.dto.UserInput;
import com.siran.itemExchange.dto.UserResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Optional;

@Path("/listings")
public class ListingResource {

    @Autowired
    ListingsRepository listingsRepository;

    @Autowired
    UsersRepository usersRepository;

    @Autowired
    CategoriesRepository categoriesRepository;

    @POST
    @Path("/add")
    @Consumes(MediaType.APPLICATION_JSON)
//    @Produces(MediaType.APPLICATION_JSON)
    public String addNewListing (@NotNull @Valid ListingInput listingInput) {

        Listings listing = new Listings();
        saveListing(listingInput, listing);
        listing.setCreatedAt(new Date());
        listing.setUpdatedAt(new Date());
        listingsRepository.save(listing);
        //return Response.status(Response.Status.CREATED).entity(ListingResponse.from(listing)).build();
        //return Response.status(Response.Status.CREATED).ok("saved item").build();
        return "listing saved";
    }

    @GET
    @Path("/{listingId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getListingById(@PathParam("listingId") Integer listingId){
        Optional<Listings> maybeListing = listingsRepository.findById(listingId);

        if (maybeListing.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        Listings listing = maybeListing.get();
        ListingResponse body = ListingResponse.from(listing);
        return Response.ok(body).build();
    }

    @PUT
    @Path("/updateById/{listingId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateListing(@PathParam("listingId") Integer id, @NotNull @Valid ListingInput input) {
        Optional<Listings> maybeListing = listingsRepository.findById(id);
        if (maybeListing.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        Listings listing = maybeListing.get();
        saveListing(input, listing);
        listing.setUpdatedAt(new Date());
        listingsRepository.save(listing);
        return Response.ok(ListingResponse.from(listing)).build();
    }

    @DELETE
    @Path("/deleteById/{listingId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteListing(@PathParam("listingId") Integer id){
        Optional<Listings> maybeListing = listingsRepository.findById(id);
        if (maybeListing.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        Listings listing = maybeListing.get();
        listingsRepository.delete(listing);
        return Response.ok().build();
    }

    //// TODO: edit enums in database; root password = root123; change apply() in UserResource to saveUser()

    private void saveListing(ListingInput in, Listings listing) {
        //Listings listing = new Listings();
        Users seller = usersRepository.findById(in.getUserId())
                .orElseThrow(() -> new BadRequestException("userId " + in.getUserId() + " does not exist"));
        listing.setUsers(seller);

        if (in.getCategoryId() == null) {
            listing.setCategories(null); // set to default 'others'
        } else {
            listing.setCategories(categoriesRepository.findById(in.getCategoryId())
                    .orElseThrow(() -> new BadRequestException("categoryId " + in.getCategoryId() + " does not exist")));
        }

        listing.setTitle(in.getTitle());
        listing.setDescription(in.getDescription() == null ? "none" : in.getDescription());
        listing.setListingType(in.getListingType());
        listing.setPrice(in.getPrice() == null ? new BigDecimal(0.00) : in.getPrice());
        listing.setConditionGrade(in.getConditionGrade());
        listing.setStatus(in.getStatus() == null ? "available" : in.getStatus());
        listing.setImageUrl(in.getImageUrl() == null ? null : in.getImageUrl());
        //return listing;
    }
}
