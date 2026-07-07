package com.salessystem.catalogservice.mapper;

import com.salessystem.catalogservice.dto.ProductDTO;
import com.salessystem.catalogservice.model.Product;
import org.mapstruct.Mapping;
import org.mapstruct.Mapper;

// This class is a mapper so that not all fields present in the database will return in the swagger
@Mapper(componentModel = "spring")
public interface ProductMapper {
    @Mapping(source = "supplier_id.name", target = "supplierName")
    @Mapping(target = "quantityInStock", source = "inventory.quantity")
    ProductDTO toDTO(Product product);

    @Mapping(target = "inventory.quantity", source = "quantityInStock")
    Product toEntity(ProductDTO dto);
}
