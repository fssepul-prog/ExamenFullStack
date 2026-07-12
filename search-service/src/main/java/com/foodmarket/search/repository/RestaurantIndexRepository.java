package com.foodmarket.search.repository;
import com.foodmarket.search.model.RestaurantIndex; import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query; import org.springframework.data.repository.query.Param; import java.util.List; import java.util.Optional;
public interface RestaurantIndexRepository extends JpaRepository<RestaurantIndex,Long> {
    Optional<RestaurantIndex> findByRestaurantId(Long restaurantId);
    List<RestaurantIndex> findByZoneAndStatus(String zone, String status);
    List<RestaurantIndex> findByCategoryAndStatus(String category, String status);
    @Query("SELECT r FROM RestaurantIndex r WHERE LOWER(r.name) LIKE LOWER(CONCAT('%',:name,'%')) AND r.status='OPEN'")
    List<RestaurantIndex> searchByName(@Param("name") String name);
    @Query("SELECT r FROM RestaurantIndex r WHERE r.zone=:zone AND r.status='OPEN' ORDER BY r.avgRating DESC")
    List<RestaurantIndex> findByZoneOrderByRating(@Param("zone") String zone);
}