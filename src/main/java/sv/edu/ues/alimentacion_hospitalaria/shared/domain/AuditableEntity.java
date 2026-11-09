package sv.edu.ues.alimentacion_hospitalaria.shared.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class AuditableEntity {

	@CreatedDate
	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@CreatedBy
	@Column(nullable = false, updatable = false, length = 100)
	private String createdBy;

	@LastModifiedDate
	@Column(nullable = false)
	private Instant updatedAt;

	@LastModifiedBy
	@Column(nullable = false, length = 100)
	private String updatedBy;
}
