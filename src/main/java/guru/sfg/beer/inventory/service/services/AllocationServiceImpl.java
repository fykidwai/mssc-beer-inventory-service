package guru.sfg.beer.inventory.service.services;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;

import guru.sfg.beer.inventory.service.domain.BeerInventory;
import guru.sfg.beer.inventory.service.repositories.BeerInventoryRepository;
import guru.sfg.brewery.model.BeerOrderDto;
import guru.sfg.brewery.model.BeerOrderLineDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class AllocationServiceImpl implements AllocationService {

    private static final int MN_0 = 0;
    private final BeerInventoryRepository beerInventoryRepository;

    @Override
    public Boolean allocateOrder(final BeerOrderDto beerOrderDto) {
        log.debug("Allocating OrderId: " + beerOrderDto.getId());

        final AtomicInteger totalOrdered = new AtomicInteger();
        final AtomicInteger totalAllocated = new AtomicInteger();

        beerOrderDto.getBeerOrderLines().forEach(beerOrderLine -> {
            if (getIntOrZero(beerOrderLine.getOrderQuantity()) > getIntOrZero(beerOrderLine.getQuantityAllocated())) {
                allocateBeerOrderLine(beerOrderLine);
            }
            totalOrdered.set(totalOrdered.get() + beerOrderLine.getOrderQuantity());
            totalAllocated.set(totalAllocated.get() + getIntOrZero(beerOrderLine.getQuantityAllocated()));
        });

        log.debug("Total Ordered: " + totalOrdered.get() + " Total Allocated: " + totalAllocated.get());

        return totalOrdered.get() == totalAllocated.get();
    }

    private void allocateBeerOrderLine(final BeerOrderLineDto beerOrderLine) {
        final List<BeerInventory> beerInventoryList = beerInventoryRepository.findAllByUpc(beerOrderLine.getUpc());

        beerInventoryList.forEach(beerInventory -> {
            int inventory = getIntOrZero(beerInventory.getQuantityOnHand());
            final int orderQty = getIntOrZero(beerOrderLine.getOrderQuantity());
            final int allocatedQty = getIntOrZero(beerOrderLine.getQuantityAllocated());
            final int qtyToAllocate = orderQty - allocatedQty;

            if (inventory >= qtyToAllocate) { // full allocation
                inventory = inventory - qtyToAllocate;
                beerOrderLine.setQuantityAllocated(orderQty);
                beerInventory.setQuantityOnHand(inventory);

                beerInventoryRepository.save(beerInventory);
            } else if (inventory > MN_0) { // partial allocation
                beerOrderLine.setQuantityAllocated(allocatedQty + inventory);
                beerInventory.setQuantityOnHand(MN_0);

                beerInventoryRepository.delete(beerInventory);
            }
        });

    }

    private int getIntOrZero(final Integer i) {
        return Optional.ofNullable(i).orElse(MN_0);
    }
}