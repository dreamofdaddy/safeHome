package com.safehome.backend.domain.buildingregister;

import java.util.List;
import com.safehome.backend.domain.buildingregister.BuildingRegisterModels.Extraction;

public interface BuildingRegisterParser {
    Extraction parse(List<byte[]> pdfs);
    String provider();
    String model();
}
