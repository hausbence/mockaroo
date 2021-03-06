package com.worldofbooks.mockaroo.validation;

import com.worldofbooks.mockaroo.entity.Listing;
import com.worldofbooks.mockaroo.entity.ListingStatus;
import com.worldofbooks.mockaroo.entity.Location;
import com.worldofbooks.mockaroo.entity.MarketPlace;
import com.worldofbooks.mockaroo.model.InvalidListingObject;
import com.worldofbooks.mockaroo.repository.ListingStatusRepository;
import com.worldofbooks.mockaroo.repository.LocationRepository;
import com.worldofbooks.mockaroo.repository.MarketPlaceRepository;
import com.worldofbooks.mockaroo.util.Util;
import lombok.Getter;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * If an object is valid it is getting added to validListingObjects
 * while if it's invalid it is getting added to invalidListingObjects
 */
@Service
public class DataValidator {

    @Autowired
    LocationRepository locationRepository;

    @Autowired
    ListingStatusRepository listingStatusRepository;

    @Autowired
    MarketPlaceRepository marketPlaceRepository;

    @Getter
    private final List<InvalidListingObject> invalidListingObjects = new ArrayList<>();


    /**
     * Validates and fill up a List with Listing objects
     * @param listingJSONArray The array with the fetched data with objects
     * @return A list with validated List objects
     */
    public List<Listing> getListWithValidElements(JSONArray listingJSONArray) throws ParseException {
        List<Listing> validListingObjects = new ArrayList<>();

        DateFormat simpleDateFormat = new SimpleDateFormat("MM/dd/yyyy");
        Location location;
        ListingStatus listingStatus;
        MarketPlace marketPlace;

        for (Object object : listingJSONArray) {
            JSONObject listingJSONObject = (JSONObject) object;
            if (isEveryStatementValid(listingJSONObject)) {
                location = locationRepository.findById(UUID.fromString((String) listingJSONObject.get("location_id"))).get();
                listingStatus = listingStatusRepository.findById(listingJSONObject.getLong("listing_status")).get();
                marketPlace = marketPlaceRepository.findById(listingJSONObject.getLong("marketplace")).get();

                Listing listingObject = Listing.builder()
                    .id(UUID.fromString((String) listingJSONObject.get("id")))
                    .upload_time(simpleDateFormat.parse((String) listingJSONObject.get("upload_time")))
                    .quantity(listingJSONObject.getDouble("quantity"))
                    .marketPlace(marketPlace)
                    .listingStatus(listingStatus)
                    .description(listingJSONObject.getString("description"))
                    .listing_price(Util.round(listingJSONObject.getDouble("listing_price"),2))
                    .currency(listingJSONObject.getString("currency"))
                    .owner_email_address(listingJSONObject.getString("owner_email_address"))
                    .title(listingJSONObject.getString("title"))
                    .location(location)
                    .build();

                validListingObjects.add(listingObject);
            }
        }
        return validListingObjects;
    }

    private boolean isEveryStatementValid(JSONObject listingJSONObject) {
        return notContainsNullElement(listingJSONObject) &&
            isValidLocationObject(listingJSONObject) &&
            isValidListingPrice(listingJSONObject) &&
            isValidCurrency(listingJSONObject) &&
            isValidQuantity(listingJSONObject) &&
            isValidEmailAddress(listingJSONObject) &&
            isValidListingStatusObject(listingJSONObject) &&
            isValidMarketplaceObject(listingJSONObject);
    }

    /**
     * Checks if null element is present in the object and calls addInvalidObject method on it.
     * @param listingJSONObject Listing object to check
     * @return False if null element is present. True if there is no null element.
     */
    private boolean notContainsNullElement(JSONObject listingJSONObject) {
        Set<String> set = listingJSONObject.keySet();
        for (String key : set) {
            if (listingJSONObject.get(key) == null || listingJSONObject.isNull(key)) {
                addInvalidObject(listingJSONObject, key);
                return false;
            }
        }
        return true;
    }

    private void addInvalidObject(JSONObject listingJSONObject, String key) {
        Object listingId = listingJSONObject.get("id");
        Optional<MarketPlace> marketPlace = marketPlaceRepository.findById(listingJSONObject.getLong("marketplace"));
        if (key.equals("marketplace")) {
            invalidListingObjects.add(new InvalidListingObject(UUID.fromString((String) listingId), "null", key));
        } else if (key.equals("id")) {
            invalidListingObjects.add(new InvalidListingObject(UUID.fromString("null"), marketPlace.get().getMarketplace_name(), key));
        } else {
            invalidListingObjects.add(new InvalidListingObject(UUID.fromString((String) listingId), marketPlace.get().getMarketplace_name(), key));
        }
    }

    public boolean isValidLocationObject(JSONObject listingJSONObject) {
        String locationId = listingJSONObject.getString("location_id");
        if (locationId == null || !locationRepository.findById(UUID.fromString(locationId)).isPresent()) {
            addInvalidObject(listingJSONObject, "location_id");
            return false;
        }
        return true;
    }

    public boolean isValidListingPrice(JSONObject listingJSONObject) {
        double listing_price = listingJSONObject.getDouble("listing_price");
        BigDecimal bigDecimalListingPrice = BigDecimal.valueOf(listing_price);

        if (listing_price <= 0 || bigDecimalListingPrice.scale() != 2) {
            addInvalidObject(listingJSONObject, "listing_price");
            return false;
        }
        return true;
    }

    public boolean isValidCurrency(JSONObject listingJSONObject) {
        if (listingJSONObject.getString("currency").length() != 3) {
            addInvalidObject(listingJSONObject, "currency");
            return false;
        }
        return true;
    }

    public boolean isValidQuantity(JSONObject listingJSONObject) {
        if (listingJSONObject.getInt("quantity") <= 0) {
            addInvalidObject(listingJSONObject, "quantity");
            return false;
        }
        return true;
    }

    public boolean isValidEmailAddress(JSONObject listingJSONObject) {
        String emailRegex = "^[\\w-_\\.+]*[\\w-_\\.]\\@([\\w]+\\.)+[\\w]+[\\w]$";
        if (!listingJSONObject.getString("owner_email_address").matches(emailRegex)) {
            addInvalidObject(listingJSONObject, "owner_email_address");
            return false;
        }
        return true;
    }

    public boolean isValidListingStatusObject(JSONObject listingJSONObject) {
        if (!listingStatusRepository.findById(listingJSONObject.getLong("listing_status")).isPresent()) {
            addInvalidObject(listingJSONObject, "listing_status");
            return false;
        }
        return true;
    }

    public boolean isValidMarketplaceObject(JSONObject listingJSONObject) {
        if (!marketPlaceRepository.findById(listingJSONObject.getLong("marketplace")).isPresent()) {
            addInvalidObject(listingJSONObject, "marketplace");
            return false;
        }
        return true;
    }

}
