package com.finance.dashboard.repository;

import com.finance.dashboard.model.Transaction;
import com.finance.dashboard.model.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    /* ──────────────────────────────────────────────────────────
       Basic finders (exclude soft-deleted)
       ────────────────────────────────────────────────────────── */

    Optional<Transaction> findByIdAndDeletedFalse(Long id);

    /**
     * Flexible listing with optional filters. All parameters may be null
     * to act as "no filter" for that dimension.
     */
    @Query("""
            SELECT t FROM Transaction t
            WHERE t.deleted = false
              AND (:type IS NULL OR t.type = :type)
              AND (:category IS NULL OR LOWER(t.category) LIKE LOWER(CONCAT('%', :category, '%')))
              AND (:startDate IS NULL OR t.date >= :startDate)
              AND (:endDate IS NULL OR t.date <= :endDate)
            ORDER BY t.date DESC, t.createdAt DESC
            """)
    Page<Transaction> findAllWithFilters(
            @Param("type") TransactionType type,
            @Param("category") String category,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable
    );

    /* ──────────────────────────────────────────────────────────
       Aggregation queries for dashboard summaries
       ────────────────────────────────────────────────────────── */

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.deleted = false AND t.type = :type")
    BigDecimal sumByType(@Param("type") TransactionType type);

    @Query("""
            SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t
            WHERE t.deleted = false
              AND t.type = :type
              AND t.date >= :startDate
              AND t.date <= :endDate
            """)
    BigDecimal sumByTypeAndDateRange(
            @Param("type") TransactionType type,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /** Returns [category, type, total] rows for category-wise breakdown. */
    @Query("""
            SELECT t.category, t.type, SUM(t.amount)
            FROM Transaction t
            WHERE t.deleted = false
            GROUP BY t.category, t.type
            ORDER BY SUM(t.amount) DESC
            """)
    List<Object[]> categoryWiseTotals();

    /** Returns [year, month, type, total] rows for monthly trends. */
    @Query("""
            SELECT YEAR(t.date), MONTH(t.date), t.type, SUM(t.amount)
            FROM Transaction t
            WHERE t.deleted = false
              AND t.date >= :since
            GROUP BY YEAR(t.date), MONTH(t.date), t.type
            ORDER BY YEAR(t.date) DESC, MONTH(t.date) DESC
            """)
    List<Object[]> monthlyTrends(@Param("since") LocalDate since);

    /** Recent N transactions across all types, ordered newest first. */
    @Query("SELECT t FROM Transaction t WHERE t.deleted = false ORDER BY t.date DESC, t.createdAt DESC")
    List<Transaction> findRecent(Pageable pageable);
}
