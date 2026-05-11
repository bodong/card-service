package xyz.pakwo.cardservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import xyz.pakwo.cardservice.dto.CardAuthorizationResponse;
import xyz.pakwo.cardservice.dto.CreateAuthorizationRequest;
import xyz.pakwo.cardservice.entity.CardAuthorization;

/**
 * @author sarwo.wibowo
 **/
@Mapper(componentModel = "spring")
public interface CardAuthorizationMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "transactionReference", ignore = true)
    @Mapping(target = "cardNumberMasked", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "riskScore", ignore = true)
    @Mapping(target = "riskLevel", ignore = true)
    @Mapping(target = "externalRiskReference", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    CardAuthorization toEntity(CreateAuthorizationRequest request);

    CardAuthorizationResponse toResponse(CardAuthorization entity);
}
