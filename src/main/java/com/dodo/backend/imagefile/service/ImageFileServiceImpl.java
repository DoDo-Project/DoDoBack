package com.dodo.backend.imagefile.service;

import com.dodo.backend.imagefile.entity.ImageFile;
import com.dodo.backend.imagefile.repository.ImageFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * {@link ImageFileService}의 구현체입니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ImageFileServiceImpl implements ImageFileService {

    private final ImageFileRepository imageFileRepository;

    /**
     * {@inheritDoc}
     * <p>
     * <b>처리 과정:</b>
     * <ol>
     * <li>입력된 펫 ID 목록이 비어있으면 빈 Map을 반환합니다.</li>
     * <li>리포지토리의 {@code findAllByPet_PetIdIn}을 호출하여 펫 ID 목록에 해당하는 이미지 파일들을 조회합니다.</li>
     * <li>조회된 엔티티에서 펫 ID와 이미지 URL을 추출하여 Map으로 변환합니다.</li>
     * </ol>
     */
    @Override
    @Transactional(readOnly = true)
    public Map<Long, String> getProfileUrlsByPetIds(List<Long> petIds) {

        if (petIds == null || petIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<ImageFile> images = imageFileRepository.findAllByPet_PetIdIn(petIds);

        return images.stream()
                .collect(Collectors.toMap(
                        img -> img.getPet().getPetId(),
                        ImageFile::getImageFileUrl
                ));
    }
}