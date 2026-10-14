package io.reflectoring.digitalwallet;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import io.reflectoring.digitalwallet.archunit.HexagonalArchitecture;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class DependencyRuleTests {

	@Test
	void validateRegistrationContextArchitecture() {
		HexagonalArchitecture.basePackage("io.reflectoring.digitalwallet")

				.withDomainLayer("application.domain")

				.withAdaptersLayer("adapter")
				.incoming("in.web")
				.outgoing("out.persistence")
				.and()

				.withApplicationLayer("application")
				.incomingPorts("port.in")
				.outgoingPorts("port.out")
				.and()

				.withConfiguration("configuration")
				.check(new ClassFileImporter()
						.importPackages("io.reflectoring.digitalwallet.."));
	}

	@Test
	void domainModelDoesNotDependOnOutside() {
		noClasses()
				.that()
				.resideInAPackage("io.reflectoring.digitalwallet.application.domain.model..")
				.should()
				.dependOnClassesThat()
				.resideOutsideOfPackages(
						"io.reflectoring.digitalwallet.application.domain.model..",
						"lombok..",
						"java.."
				)
				.check(new ClassFileImporter()
						.importPackages("io.reflectoring.digitalwallet.."));
	}

}
