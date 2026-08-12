package com.zanh.route_sharing.security.boarding;

import com.zanh.route_sharing.security.boarding.model.BoardingCodeBinding;
import com.zanh.route_sharing.security.boarding.model.ProtectedBoardingCode;

public interface BoardingCodeProtector {
    ProtectedBoardingCode protect(String boardingCode, BoardingCodeBinding binding);

    String reveal(ProtectedBoardingCode protectedCode, BoardingCodeBinding binding);
}
