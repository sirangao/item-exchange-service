package com.siran.itemExchange.resource;

import com.siran.itemExchange.dataObjects.Listings;
import com.siran.itemExchange.dataObjects.Users;
import com.siran.itemExchange.dataRepositories.CategoriesRepository;
import com.siran.itemExchange.dataRepositories.ListingsRepository;
import com.siran.itemExchange.dataRepositories.UsersRepository;
import com.siran.itemExchange.dto.ListingInput;
import com.siran.itemExchange.dto.ListingResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Path("/listings")
public class ListingResource {

    @Autowired
    ListingsRepository listingsRepository;

    @Autowired
    UsersRepository usersRepository;

    @Autowired
    CategoriesRepository categoriesRepository;

    /**
     * Browse. Defaults to available listings only — sold and exchanged items stay out of
     * the marketplace but remain visible to their owner through /listings/user/{userId}.
     * category is matched by name because that is what the frontend's filter sends.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response browseListings(@QueryParam("type") String type,
                                   @QueryParam("category") String category,
                                   @DefaultValue("available") @QueryParam("status") String status) {

        List<ListingResponse> body = listingsRepository
                .browse(status, blankToNull(type), blankToNull(category))
                .stream()
                .map(ListingResponse::from)
                .toList();

        return Response.ok(body).build();
    }

    @GET
    @Path("/user/{userId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getListingsByUser(@PathParam("userId") Integer userId) {
        if (!usersRepository.existsById(userId)) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        List<ListingResponse> body = listingsRepository.findByUserIdWithDetails(userId)
                .stream()
                .map(ListingResponse::from)
                .toList();

        return Response.ok(body).build();
    }

    @POST
    @Path("/add")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addNewListing(@NotNull @Valid ListingInput listingInput) {

        Listings listing = new Listings();
        saveListing(listingInput, listing);
        listing.setCreatedAt(new Date());
        listing.setUpdatedAt(new Date());
        listingsRepository.save(listing);

        // The client navigates straight to the new listing, so it needs the generated id.
        return Response.status(Response.Status.CREATED).entity(ListingResponse.from(listing)).build();
    }

    @GET
    @Path("/{listingId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getListingById(@PathParam("listingId") Integer listingId){
        Optional<Listings> maybeListing = listingsRepository.findByIdWithDetails(listingId);

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

    private void saveListing(ListingInput in, Listings listing) {
        // findById returns a fully loaded entity rather than a lazy proxy, which is what
        // lets ListingResponse.from() read the seller's username on the way back out.
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
    }

    /** An unset filter arrives as an empty string, which must not narrow the query. */
    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
