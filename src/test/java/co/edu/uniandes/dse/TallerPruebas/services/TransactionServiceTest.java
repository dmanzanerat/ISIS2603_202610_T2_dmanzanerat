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
import co.edu.uniandes.dse.TallerPruebas.entities.TransactionEntity;
import co.edu.uniandes.dse.TallerPruebas.exceptions.BusinessLogicException;
import co.edu.uniandes.dse.TallerPruebas.exceptions.EntityNotFoundException;
import uk.co.jemos.podam.api.PodamFactory;
import uk.co.jemos.podam.api.PodamFactoryImpl;

@DataJpaTest
@Transactional
@Import(TransactionService.class)
public class TransactionServiceTest {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private TestEntityManager entityManager;

    private PodamFactory factory = new PodamFactoryImpl();

    private List<AccountEntity> accountList = new ArrayList<>();

    @BeforeEach
    void setUp() {
        clearData();
        insertData();
    }

    private void clearData() {
        entityManager.getEntityManager().createQuery("delete from TransactionEntity").executeUpdate();
        entityManager.getEntityManager().createQuery("delete from AccountEntity").executeUpdate();
    }

    private void insertData() {
        for (int i = 0; i < 3; i++) {
            AccountEntity accountEntity = factory.manufacturePojo(AccountEntity.class);
            accountEntity.setEstado("ACTIVA");
            accountEntity.setSaldo(5000.0);
            entityManager.persist(accountEntity);
            accountList.add(accountEntity);
        }
    }

    /**
     * Prueba ambas cuentas existen y son distintas.
     */
    @Test
    void testTransferirFondos() throws EntityNotFoundException, BusinessLogicException {
        AccountEntity source = accountList.get(0);
        AccountEntity destination = accountList.get(1);
        Double amount = 1500.0;

        TransactionEntity result = transactionService.transferirFondos(source.getId(), destination.getId(), amount);

        assertNotNull(result);
        AccountEntity updatedSource = entityManager.find(AccountEntity.class, source.getId());
        AccountEntity updatedDestination = entityManager.find(AccountEntity.class, destination.getId());

        assertEquals(3500.0, updatedSource.getSaldo());
        assertEquals(6500.0, updatedDestination.getSaldo());
        assertEquals("SALIDA", result.getTipo());
    }

    /**
     * Prueba: Fallo: Saldo de la cuenta origen es menor al monto.
     */
    @Test
    void testTransferirFondosInsuficientes() {
        assertThrows(BusinessLogicException.class, () -> {
            AccountEntity source = accountList.get(0);
            AccountEntity destination = accountList.get(1);
            transactionService.transferirFondos(source.getId(), destination.getId(), 6000.0);
        });
    }

    /**
     * Prueba cuenta origen no existe.
     */
    @Test
    void testTransferirFondosOrigenInexistente() {
        assertThrows(EntityNotFoundException.class, () -> {
            AccountEntity destination = accountList.get(1);
            // When: El ID de origen no existe (0L)
            transactionService.transferirFondos(0L, destination.getId(), 1000.0);
        });
    }

    /**
     * Prueba cuenta destino no existe.
     */
    @Test
    void testTransferirFondosDestinoInexistente() {
        assertThrows(EntityNotFoundException.class, () -> {
            AccountEntity source = accountList.get(0);
            // When: El ID de destino no existe (0L)
            transactionService.transferirFondos(source.getId(), 0L, 1000.0);
        });
    }

    /**
     * Prueba cuenta origen y destino son la misma.
     */
    @Test
    void testTransferirFondosMismaCuenta() {
        assertThrows(BusinessLogicException.class, () -> {
            AccountEntity source = accountList.get(0);
            transactionService.transferirFondos(source.getId(), source.getId(), 1000.0);
        });
    }
}


