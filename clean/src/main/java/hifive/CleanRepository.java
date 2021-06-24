package hifive;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel="cleans", path="cleans")
public interface CleanRepository extends PagingAndSortingRepository<Clean, Long>{
    public Clean findByRoomNumber(Long roomnumber);

}
