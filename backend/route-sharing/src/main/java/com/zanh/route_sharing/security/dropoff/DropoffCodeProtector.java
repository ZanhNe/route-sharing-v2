package com.zanh.route_sharing.security.dropoff;

import com.zanh.route_sharing.security.dropoff.model.DropoffCodeBinding;
import com.zanh.route_sharing.security.dropoff.model.ProtectedDropoffCode;

public interface DropoffCodeProtector {
    ProtectedDropoffCode protect(String code, DropoffCodeBinding binding);
    String reveal(ProtectedDropoffCode protectedCode, DropoffCodeBinding binding);
}
