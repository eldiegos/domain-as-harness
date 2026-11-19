package io.github.eldiegos.domainasharness.pagos;

import io.github.eldiegos.domainasharness.dominio.EnrollmentId;
import io.github.eldiegos.domainasharness.dominio.Payments;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/** Un sistema de pagos en memoria para el ejemplo. En producción sería una pasarela de pago. */
@Component
public class PaymentsRegistry implements Payments {
	private final Set<Long> paid = new HashSet<>();

	public synchronized void markPaid(long enrollmentId) {
		paid.add(enrollmentId);
	}

	@Override
	public synchronized boolean isPaid(EnrollmentId enrollment) {
		return paid.contains(enrollment.value());
	}
}
