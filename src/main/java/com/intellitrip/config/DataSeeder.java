package com.intellitrip.config;

import com.intellitrip.model.Notification;
import com.intellitrip.model.SavedPlace;
import com.intellitrip.model.Trip;
import com.intellitrip.model.User;
import com.intellitrip.repository.NotificationRepository;
import com.intellitrip.repository.SavedPlaceRepository;
import com.intellitrip.repository.TripRepository;
import com.intellitrip.repository.UserRepository;
import com.intellitrip.service.CurrencyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final TripRepository tripRepository;
    private final SavedPlaceRepository savedPlaceRepository;
    private final NotificationRepository notificationRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrencyService currencyService;

    public DataSeeder(UserRepository userRepository, TripRepository tripRepository, SavedPlaceRepository savedPlaceRepository, NotificationRepository notificationRepository, PasswordEncoder passwordEncoder, CurrencyService currencyService) {
        this.userRepository = userRepository;
        this.tripRepository = tripRepository;
        this.savedPlaceRepository = savedPlaceRepository;
        this.notificationRepository = notificationRepository;
        this.passwordEncoder = passwordEncoder;
        this.currencyService = currencyService;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            log.info("Seeding database with demo users...");

            User marcus = new User("Marcus Chen", "marcus@example.com", passwordEncoder.encode("password"));
            marcus.setCountry("Japan");
            marcus.setCountryCode("JP");
            userRepository.save(marcus);

            User zara = new User("Zara Okafor", "zara@example.com", passwordEncoder.encode("password"));
            zara.setCountry("France");
            zara.setCountryCode("FR");
            userRepository.save(zara);

            User diego = new User("Diego Alvarez", "diego@example.com", passwordEncoder.encode("password"));
            diego.setCountry("Italy");
            diego.setCountryCode("IT");
            userRepository.save(diego);

            User priya = new User("Priya Sharma", "priya@example.com", passwordEncoder.encode("password"));
            userRepository.save(priya);

            User tom = new User("Tom Becker", "tom@example.com", passwordEncoder.encode("password"));
            tom.setCountry("Thailand");
            tom.setCountryCode("TH");
            userRepository.save(tom);

            log.info("Database seeded with {} users", userRepository.count());
        } else {
            log.info("Users already exist ({}), skipping user seeding", userRepository.count());
        }

        if (tripRepository.count() == 0) {
            log.info("Seeding demo trips...");
            seedTrips();
        } else {
            log.info("Trips already exist ({}), skipping trip seeding", tripRepository.count());
        }

        if (savedPlaceRepository.count() == 0) {
            log.info("Seeding demo saved places...");
            seedSavedPlaces();
        } else {
            log.info("Saved places already exist ({}), skipping saved place seeding", savedPlaceRepository.count());
        }

        if (notificationRepository.count() == 0) {
            log.info("Seeding demo notifications...");
            seedNotifications();
        } else {
            log.info("Notifications already exist ({}), skipping notification seeding", notificationRepository.count());
        }
    }

    private void seedTrips() {
        List<User> users = userRepository.findAll();
        if (users.isEmpty()) {
            log.warn("No users found for trip seeding");
            return;
        }

        User marcus = users.stream().filter(u -> "marcus@example.com".equals(u.getEmail())).findFirst().orElse(users.get(0));
        User zara = users.stream().filter(u -> "zara@example.com".equals(u.getEmail())).findFirst().orElse(users.get(1));
        User diego = users.stream().filter(u -> "diego@example.com".equals(u.getEmail())).findFirst().orElse(users.get(2));
        User tom = users.stream().filter(u -> "tom@example.com".equals(u.getEmail())).findFirst().orElse(users.size() > 3 ? users.get(3) : users.get(0));
        Trip upcoming1 = new Trip();
        upcoming1.setUser(marcus);
        upcoming1.setDestination("Kyoto");
        upcoming1.setCountry("Japan");
        upcoming1.setImage("/images/image1.jpeg");
        upcoming1.setDays(5);
        upcoming1.setBudget("Moderate");
        upcoming1.setBudgetUsd(1200 / currencyService.usdRateForCurrency("JPY"));
        upcoming1.setTravelType("Solo");
        upcoming1.setInterests("culture, food");
        upcoming1.setDestinationCurrencyCode("JPY");
        upcoming1.setItineraryJson("{\"title\":\"Kyoto Cultural Escape\",\"overview\":\"5 days in Kyoto\",\"totalBudget\":1200,\"days\":[]}");
        upcoming1.setStatus("upcoming");
        upcoming1.setStartDate(LocalDateTime.now().plusDays(7).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        upcoming1.setCreatedAt(LocalDateTime.now());
        tripRepository.save(upcoming1);

        Trip completed1 = new Trip();
        completed1.setUser(marcus);
        completed1.setDestination("Bali");
        completed1.setCountry("Indonesia");
        completed1.setImage("/images/image2.jpeg");
        completed1.setDays(7);
        completed1.setBudget("Luxury");
        completed1.setBudgetUsd(2500 / currencyService.usdRateForCurrency("IDR"));
        completed1.setTravelType("Couple");
        completed1.setInterests("nature, food");
        completed1.setDestinationCurrencyCode("IDR");
        completed1.setItineraryJson("{\"title\":\"Bali Bliss\",\"overview\":\"7 days in Bali\",\"totalBudget\":2500,\"days\":[]}");
        completed1.setStatus("completed");
        completed1.setStartDate(LocalDateTime.now().minusDays(30).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        completed1.setCreatedAt(LocalDateTime.now().minusDays(45));
        tripRepository.save(completed1);

        Trip upcoming2 = new Trip();
        upcoming2.setUser(zara);
        upcoming2.setDestination("Paris");
        upcoming2.setCountry("France");
        upcoming2.setImage("/images/image3.jpeg");
        upcoming2.setDays(4);
        upcoming2.setBudget("Luxury");
        upcoming2.setBudgetUsd(3000 / currencyService.usdRateForCurrency("EUR"));
        upcoming2.setTravelType("Couple");
        upcoming2.setInterests("culture, food, nightlife");
        upcoming2.setDestinationCurrencyCode("EUR");
        upcoming2.setItineraryJson("{\"title\":\"Paris Romance\",\"overview\":\"4 days in Paris\",\"totalBudget\":3000,\"days\":[]}");
        upcoming2.setStatus("upcoming");
        upcoming2.setStartDate(LocalDateTime.now().plusDays(14).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        upcoming2.setCreatedAt(LocalDateTime.now());
        tripRepository.save(upcoming2);

        Trip completed2 = new Trip();
        completed2.setUser(zara);
        completed2.setDestination("Istanbul");
        completed2.setCountry("Turkey");
        completed2.setImage("/images/image4.jpeg");
        completed2.setDays(6);
        completed2.setBudget("Moderate");
        completed2.setBudgetUsd(1500 / currencyService.usdRateForCurrency("TRY"));
        completed2.setTravelType("Family");
        completed2.setInterests("culture, adventure");
        completed2.setDestinationCurrencyCode("TRY");
        completed2.setItineraryJson("{\"title\":\"Istanbul Discovery\",\"overview\":\"6 days in Istanbul\",\"totalBudget\":1500,\"days\":[]}");
        completed2.setStatus("completed");
        completed2.setStartDate(LocalDateTime.now().minusDays(60).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        completed2.setCreatedAt(LocalDateTime.now().minusDays(75));
        tripRepository.save(completed2);

        Trip upcoming3 = new Trip();
        upcoming3.setUser(diego);
        upcoming3.setDestination("Rome");
        upcoming3.setCountry("Italy");
        upcoming3.setImage("/images/image5.jpeg");
        upcoming3.setDays(5);
        upcoming3.setBudget("Moderate");
        upcoming3.setBudgetUsd(1800 / currencyService.usdRateForCurrency("EUR"));
        upcoming3.setTravelType("Friends");
        upcoming3.setInterests("culture, food, nightlife");
        upcoming3.setDestinationCurrencyCode("EUR");
        upcoming3.setItineraryJson("{\"title\":\"Roman Holiday\",\"overview\":\"5 days in Rome\",\"totalBudget\":1800,\"days\":[]}");
        upcoming3.setStatus("upcoming");
        upcoming3.setStartDate(LocalDateTime.now().plusDays(3).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        upcoming3.setCreatedAt(LocalDateTime.now());
        tripRepository.save(upcoming3);

        Trip completed3 = new Trip();
        completed3.setUser(tom);
        completed3.setDestination("Phuket");
        completed3.setCountry("Thailand");
        completed3.setImage("/images/image6.jpeg");
        completed3.setDays(5);
        completed3.setBudget("Budget Friendly");
        completed3.setBudgetUsd(700 / currencyService.usdRateForCurrency("THB"));
        completed3.setTravelType("Family");
        completed3.setInterests("nature, adventure");
        completed3.setDestinationCurrencyCode("THB");
        completed3.setItineraryJson("{\"title\":\"Phuket Paradise\",\"overview\":\"5 days in Phuket\",\"totalBudget\":700,\"days\":[]}");
        completed3.setStatus("completed");
        completed3.setStartDate(LocalDateTime.now().minusDays(20).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        completed3.setCreatedAt(LocalDateTime.now().minusDays(35));
        tripRepository.save(completed3);

        log.info("Seeded {} demo trips", tripRepository.count());
    }

    private void seedSavedPlaces() {
        List<User> users = userRepository.findAll();
        if (users.isEmpty()) {
            log.warn("No users found for saved place seeding");
            return;
        }

        User marcus = users.stream().filter(u -> "marcus@example.com".equals(u.getEmail())).findFirst().orElse(users.get(0));
        User zara = users.stream().filter(u -> "zara@example.com".equals(u.getEmail())).findFirst().orElse(users.get(1));

        SavedPlace place1 = new SavedPlace();
        place1.setUser(marcus);
        place1.setName("Fushimi Inari Taisha");
        place1.setLocation("Kyoto, Japan");
        place1.setRating(4.8);
        place1.setPrice("Free");
        place1.setImage("/images/image1.jpeg");
        place1.setCategory("Temple");
        place1.setDescription("Famous Shinto shrine known for its thousands of vermilion torii gates.");
        savedPlaceRepository.save(place1);

        SavedPlace place2 = new SavedPlace();
        place2.setUser(zara);
        place2.setName("Eiffel Tower");
        place2.setLocation("Paris, France");
        place2.setRating(4.7);
        place2.setPrice("€25");
        place2.setImage("/images/image3.jpeg");
        place2.setCategory("Landmark");
        place2.setDescription("Iconic iron tower and the most recognizable landmark in Paris.");
        savedPlaceRepository.save(place2);

        log.info("Seeded {} demo saved places", savedPlaceRepository.count());
    }

    private void seedNotifications() {
        List<User> users = userRepository.findAll();
        if (users.isEmpty()) {
            log.warn("No users found for notification seeding");
            return;
        }

        User marcus = users.stream().filter(u -> "marcus@example.com".equals(u.getEmail())).findFirst().orElse(users.get(0));
        User zara = users.stream().filter(u -> "zara@example.com".equals(u.getEmail())).findFirst().orElse(users.get(1));

        Notification notif1 = new Notification();
        notif1.setUser(marcus);
        notif1.setTitle("Welcome to intellitrip");
        notif1.setMessage("Start planning your dream trip to Kyoto today!");
        notif1.setType("welcome");
        notif1.setRead(false);
        notificationRepository.save(notif1);

        Notification notif2 = new Notification();
        notif2.setUser(zara);
        notif2.setTitle("Trip Reminder");
        notif2.setMessage("Your Paris trip is coming up in 14 days. Review your itinerary.");
        notif2.setType("reminder");
        notif2.setRead(false);
        notificationRepository.save(notif2);

        log.info("Seeded {} demo notifications", notificationRepository.count());
    }
}
