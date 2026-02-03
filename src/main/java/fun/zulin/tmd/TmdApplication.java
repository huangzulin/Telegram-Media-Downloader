package fun.zulin.tmd;

import com.tangzc.autotable.springboot.EnableAutoTable;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@EnableAutoTable
@SpringBootApplication
public class TmdApplication {


    public static void main(String[] args) {
        SpringApplication.run(TmdApplication.class, args);
    }

}
