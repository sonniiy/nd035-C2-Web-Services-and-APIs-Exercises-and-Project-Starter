package com.udacity.vehicles.service;

import com.udacity.vehicles.client.maps.MapsClient;
import com.udacity.vehicles.client.prices.PriceClient;
import com.udacity.vehicles.domain.Location;
import com.udacity.vehicles.domain.car.Car;
import com.udacity.vehicles.domain.car.CarRepository;

import java.time.LocalDate;
import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Implements the car service create, read, update or delete
 * information about vehicles, as well as gather related
 * location and price data when desired.
 */
@Service
public class CarService {

    private final CarRepository repository;

    public final WebClient webClientMaps;
    public final WebClient webClientPricing;

    private final ModelMapper modelMapper;

    public CarService(CarRepository repository, @Qualifier("maps") WebClient webClientMaps,
                      @Qualifier("pricing") WebClient webClientPricing, ModelMapper modelMapper) {

        this.webClientMaps = webClientMaps;
        this.repository = repository;
        this.webClientPricing = webClientPricing;
        this.modelMapper = modelMapper;
    }

    /**
     * Gathers a list of all vehicles
     * @return a list of all vehicles in the CarRepository
     */
    public List<Car> list() {
        return repository.findAll();
    }

    /**
     * Gets car information by ID (or throws exception if non-existent)
     * @param id the ID number of the car to gather information on
     * @return the requested car's information, including location and price
     */
    public Car findById(Long id) {

        Car car = repository.findById(id)
                .orElseThrow(CarNotFoundException::new);

        /**
         *
         * Note: The car class file uses @transient, meaning you will need to call
         *   the pricing service each time to get the price.
         */

        PriceClient priceClient = new PriceClient(webClientPricing);
        String price = priceClient.getPrice(id);

        car.setPrice(price);


        /**
         * Note: The Location class file also uses @transient for the address,
         * meaning the Maps service needs to be called each time for the address.
         */

        MapsClient mapsClient = new MapsClient(webClientMaps, modelMapper);
        Location location = car.getLocation();
        Location newLocation = mapsClient.getAddress(location);

        car.setLocation(newLocation);

        return car;
    }

    /**
     * Either creates or updates a vehicle, based on prior existence of car
     * @param car A car object, which can be either new or existing
     * @return the new/updated car is stored in the repository
     */
    public Car save(Car car) {
        if (car.getId() != null) {
            return repository.findById(car.getId())
                    .map(carToBeUpdated -> {
                        carToBeUpdated.setDetails(car.getDetails());
                        carToBeUpdated.setLocation(car.getLocation());
                        carToBeUpdated.setCondition(car.getCondition());
                        return repository.save(carToBeUpdated);
                    }).orElseThrow(CarNotFoundException::new);
        }

        return repository.save(car);
    }

    /**
     * Deletes a given car by ID
     * @param id the ID number of the car to delete
     */
    public void delete(Long id) {

        Car car = repository.findById(id)
                .orElseThrow(CarNotFoundException::new);

        repository.delete(car);

    }
}
