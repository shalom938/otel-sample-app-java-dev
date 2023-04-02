package org.springframework.samples.petclinic.domain;

import org.springframework.samples.petclinic.owner.Owner;

public class TwoFactorAuthenticationService {
	public boolean init2FA(Owner usr) {
		return true;
	}

	public String getTokenInput() {
			return "";
	}

	public boolean vldtToken(Owner usr, String token) {
		return true;
	}
}
