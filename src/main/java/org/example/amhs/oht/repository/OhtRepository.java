package org.example.amhs.oht.repository;

import java.util.List;
import org.example.amhs.oht.domain.Oht;
import org.example.amhs.oht.domain.OhtStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OhtRepository extends JpaRepository<Oht, String> {

    List<Oht> findByStatusAndCurrentRequestIdIsNull(OhtStatus status);
}
