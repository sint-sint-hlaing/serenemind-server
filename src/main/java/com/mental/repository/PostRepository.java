package com.mental.repository;


import com.mental.model.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    // Recent filter အတွက် (နောက်ဆုံးတင်တဲ့ ပို့စ်များကို အရင်ပြရန်)
    List<Post> findAllByOrderByCreatedAtDesc();

    // Popular filter အတွက် (Like အများဆုံး ပို့စ်များကို အရင်ပြရန်)
    List<Post> findAllByOrderByLikeCountDesc();
}
