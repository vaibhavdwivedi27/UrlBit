package in.urlbit.links.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class Base62CodeGeneratorTest {

    private Base62CodeGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new Base62CodeGenerator();
    }

    @Test
    void generatedCodeHasLengthSeven() {
        String code = generator.generate();
        assertThat(code).hasSize(7);
    }

    @Test
    void generatedCodeContainsOnlyBase62Characters() {
        for (int i = 0; i < 50; i++) {
            String code = generator.generate();
            assertThat(code).matches("^[0-9A-Za-z]{7}$");
        }
    }

    @Test
    void consecutiveCodesAreUnique() {
        Set<String> generated = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            String code = generator.generate();
            assertThat(generated.add(code))
                    .withFailMessage("Duplicate code generated: " + code)
                    .isTrue();
        }
    }
}
