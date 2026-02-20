package co.edu.uniandes.dse.TallerPruebas.services;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import jakarta.transaction.Transactional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import co.edu.uniandes.dse.TallerPruebas.entities.AccountEntity;
import co.edu.uniandes.dse.TallerPruebas.entities.PocketEntity;
import co.edu.uniandes.dse.TallerPruebas.exceptions.BusinessLogicException;
import co.edu.uniandes.dse.TallerPruebas.exceptions.EntityNotFoundException;
import uk.co.jemos.podam.api.PodamFactory;
import uk.co.jemos.podam.api.PodamFactoryImpl;

/**
 * Pruebas de lógica de PocketService
 */
@DataJpaTest
@Transactional
@Import(PocketService.class)
public class PocketServiceTest {

    @Autowired
    private PocketService pocketService;

    @Autowired
    private TestEntityManager entityManager;

    private PodamFactory factory = new PodamFactoryImpl();

    private List<AccountEntity> accountList = new ArrayList<>();
    private List<PocketEntity> pocketList = new ArrayList<>();

    /**
     * Configuración inicial de la prueba.
     */
    @BeforeEach
    void setUp() {
        clearData();
        insertData();
    }

    /**
     * Limpia las tablas que están implicadas en la prueba.
     */
    private void clearData() {
        entityManager.getEntityManager().createQuery("delete from PocketEntity").executeUpdate();
        entityManager.getEntityManager().createQuery("delete from AccountEntity").executeUpdate();
    }

    /**
     * Inserta datos iniciales para el correcto funcionamiento de las pruebas.
     */
    private void insertData() {
        for (int i = 0; i < 3; i++) {
            AccountEntity accountEntity = factory.manufacturePojo(AccountEntity.class);
            accountEntity.setEstado("ACTIVA");
            entityManager.persist(accountEntity);
            accountList.add(accountEntity);
        }
        for (int i = 0; i < 3; i++) {
            PocketEntity pocketEntity = factory.manufacturePojo(PocketEntity.class);
            pocketEntity.setAccount(accountList.get(0));
            entityManager.persist(pocketEntity);
            pocketList.add(pocketEntity);
        }
        // Actualizar la lista de bolsillos en la cuenta para las validaciones
        accountList.get(0).setPockets(pocketList);
    }

    /**
     * Prueba para crear un Pocket.
     */
    @Test
    void testCreatePocket() throws EntityNotFoundException, BusinessLogicException {
        PocketEntity newEntity = factory.manufacturePojo(PocketEntity.class);
        newEntity.setNombre("Nuevo Bolsillo");
        AccountEntity account = accountList.get(1); // Una cuenta sin bolsillos
        
        PocketEntity result = pocketService.createPocket(account.getId(), newEntity);
        
        assertNotNull(result);
        PocketEntity entity = entityManager.find(PocketEntity.class, result.getId());
        assertEquals(newEntity.getId(), entity.getId());
        assertEquals(newEntity.getNombre(), entity.getNombre());
    }

    /**
     * Prueba para crear un Pocket con una cuenta que no existe.
     */
    @Test
    void testCreatePocketWithInvalidAccount() {
        assertThrows(EntityNotFoundException.class, () -> {
            PocketEntity newEntity = factory.manufacturePojo(PocketEntity.class);
            pocketService.createPocket(0L, newEntity);
        });
    }

    /**
     * Prueba para crear un Pocket con una cuenta bloqueada.
     */
    @Test
    void testCreatePocketWithBlockedAccount() {
        assertThrows(BusinessLogicException.class, () -> {
            AccountEntity account = accountList.get(2);
            account.setEstado("BLOQUEADA");
            entityManager.merge(account);
            
            PocketEntity newEntity = factory.manufacturePojo(PocketEntity.class);
            pocketService.createPocket(account.getId(), newEntity);
        });
    }

    /**
     * Prueba para crear un Pocket con un nombre ya existente en la cuenta.
     */
    @Test
    void testCreatePocketWithDuplicateName() {
        assertThrows(BusinessLogicException.class, () -> {
            AccountEntity account = accountList.get(0);
            PocketEntity newEntity = factory.manufacturePojo(PocketEntity.class);
            newEntity.setNombre(pocketList.get(0).getNombre());
            
            pocketService.createPocket(account.getId(), newEntity);
        });
    }

    /**
     * Prueba para mover dinero de la cuenta a un bolsillo con éxito.
     */
    @Test
    void testCargarBolsillo() throws EntityNotFoundException, BusinessLogicException {
        AccountEntity account = accountList.get(0);
        Double initialAccountBalance = 5000.0;
        account.setSaldo(initialAccountBalance);
        entityManager.merge(account);

        PocketEntity pocket = pocketList.get(0);
        Double initialPocketBalance = 0.0;
        pocket.setSaldo(initialPocketBalance);
        entityManager.merge(pocket);

        Double amountToLoad = 1500.0;

        PocketEntity result = pocketService.cargarBolsillo(account.getId(), pocket.getId(), amountToLoad);

        AccountEntity updatedAccount = entityManager.find(AccountEntity.class, account.getId());
        assertEquals(initialAccountBalance - amountToLoad, updatedAccount.getSaldo());
        assertEquals(initialPocketBalance + amountToLoad, result.getSaldo());
    }

    /**
     * Prueba para mover dinero con saldo insuficiente en la cuenta.
     */
    @Test
    void testCargarBolsilloFondosInsuficientes() {
        assertThrows(BusinessLogicException.class, () -> {
            AccountEntity account = accountList.get(0);
            account.setSaldo(5000.0);
            entityManager.merge(account);

            PocketEntity pocket = pocketList.get(0);
            Double amountToLoad = 6000.0;

            pocketService.cargarBolsillo(account.getId(), pocket.getId(), amountToLoad);
        });
    }

    /**
     * Prueba para mover dinero a un bolsillo que no existe.
     */
    @Test
    void testCargarBolsilloInexistente() {
        assertThrows(EntityNotFoundException.class, () -> {
            AccountEntity account = accountList.get(0);
            pocketService.cargarBolsillo(account.getId(), 0L, 1500.0);
        });
    }

    /**
     * Prueba para mover un monto negativo.
     */
    @Test
    void testCargarBolsilloMontoNegativo() {
        assertThrows(BusinessLogicException.class, () -> {
            AccountEntity account = accountList.get(0);
            PocketEntity pocket = pocketList.get(0);

            pocketService.cargarBolsillo(account.getId(), pocket.getId(), -500.0);
        });
    }

    /**
     * Prueba para mover dinero desde una cuenta que no existe.
     */
    @Test
    void testCargarBolsilloCuentaInexistente() {
        assertThrows(EntityNotFoundException.class, () -> {
            PocketEntity pocket = pocketList.get(0);
            pocketService.cargarBolsillo(0L, pocket.getId(), 100.0);
        });
    }
}
