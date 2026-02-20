package co.edu.uniandes.dse.TallerPruebas.services;

import java.util.Optional;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uniandes.dse.TallerPruebas.entities.AccountEntity;
import co.edu.uniandes.dse.TallerPruebas.entities.TransactionEntity;
import co.edu.uniandes.dse.TallerPruebas.exceptions.BusinessLogicException;
import co.edu.uniandes.dse.TallerPruebas.exceptions.EntityNotFoundException;
import co.edu.uniandes.dse.TallerPruebas.repositories.AccountRepository;
import co.edu.uniandes.dse.TallerPruebas.repositories.TransactionRepository;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class TransactionService {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    /**
     * Realiza una transferencia entre dos cuentas.
     * @param sourceId ID de la cuenta origen
     * @param destinationId ID de la cuenta destino
     * @param monto Cantidad a transferir
     * @return La entidad de la transacción creada en la cuenta origen
     * @throws EntityNotFoundException Si alguna de las cuentas no existe
     * @throws BusinessLogicException Si son la misma cuenta, fondos insuficientes o monto inválido
     */
    @Transactional
    public TransactionEntity transferirFondos(Long sourceId, Long destinationId, Double monto)
            throws EntityNotFoundException, BusinessLogicException {

        log.info("Inicia proceso de transferencia de {} desde {} hacia {}", monto, sourceId, destinationId);

        // 1. validar monto positivo
        if (monto <= 0 || monto.isInfinite()) {
            throw new BusinessLogicException("El monto de la transferencia debe ser mayor que cero");
        }

        // 2. validar que la cuenta origen existe
        Optional<AccountEntity> sourceAccount = accountRepository.findById(sourceId);
        if (sourceAccount.isEmpty()) {
            throw new EntityNotFoundException("La cuenta origen no existe");
        }

        // 3. validar que la cuenta destino exista
        Optional<AccountEntity> destinationAccount = accountRepository.findById(destinationId);
        if (destinationAccount.isEmpty()) {
            throw new EntityNotFoundException("La cuenta destino no existe");
        }

        // 4. validar que la cuenta origen no sea la misma que la destino
        if (sourceId.equals(destinationId)) {
            throw new BusinessLogicException("La cuenta destino debe ser diferente a la cuenta origen");
        }

        // 5. validar fondos suficientes en origen
        if (sourceAccount.get().getSaldo() < monto) {
            throw new BusinessLogicException("El monto de la transferencia debe ser menor o igual al saldo de la cuenta");
        }

        // 6. actualizar ambos saldos
        sourceAccount.get().setSaldo(sourceAccount.get().getSaldo() - monto);
        destinationAccount.get().setSaldo(destinationAccount.get().getSaldo() + monto);

        // 7. guardar cambios en las cuentas
        accountRepository.save(sourceAccount.get());
        accountRepository.save(destinationAccount.get());

        // 8. crear el registro de la transacción para el historial
        TransactionEntity transaction = new TransactionEntity();
        transaction.setMonto(monto);
        transaction.setFecha(new Date());
        transaction.setTipo("SALIDA");
        transaction.setAccount(sourceAccount.get());

        log.info("Transferencia completada exitosamente");
        return transactionRepository.save(transaction);
    }
}
