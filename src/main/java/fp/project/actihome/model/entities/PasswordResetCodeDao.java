package fp.project.actihome.model.entities;

import java.util.ArrayList;

import org.springframework.data.repository.PagingAndSortingRepository;

public interface PasswordResetCodeDao extends PagingAndSortingRepository<PasswordResetCode, Long> {

	/**
	 * Los códigos vivos de un usuario, el más reciente primero.
	 *
	 * <p>
	 * Devuelve todos y no solo el último a propósito: pedir un código nuevo anula
	 * los anteriores, y para anularlos hay que tenerlos. Si solo se mirara el
	 * último, los viejos seguirían siendo válidos hasta caducar — y quien pide tres
	 * códigos seguidos porque el correo tarda acabaría con tres llaves activas.
	 */
	ArrayList<PasswordResetCode> findByUserIdAndUsedAtIsNullOrderByExpiresAtDesc(Long userId);
}
