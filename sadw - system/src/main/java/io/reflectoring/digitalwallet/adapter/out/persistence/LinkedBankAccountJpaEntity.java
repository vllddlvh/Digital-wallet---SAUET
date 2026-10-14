package io.reflectoring.digitalwallet.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "linked_bank_account")
@Data
@AllArgsConstructor
@NoArgsConstructor
class LinkedBankAccountJpaEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long accountId;

	@Column(nullable = false)
	private String bankCode;

	@Column(nullable = false)
	private String bankName;

	@Column(nullable = false)
	private String accountNumber;

	@Column(nullable = false)
	private String accountHolderName;

	@Column(nullable = false)
	private String status;

	@Column(nullable = false)
	private LocalDateTime linkedAt;

	@Column
	private LocalDateTime unlinkedAt;
}
