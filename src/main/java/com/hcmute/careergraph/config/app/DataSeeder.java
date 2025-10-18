package com.hcmute.careergraph.config.app;

import com.github.javafaker.Faker;
import com.hcmute.careergraph.enums.work.EmploymentType;
import com.hcmute.careergraph.enums.work.JobCategory;
import com.hcmute.careergraph.enums.common.Status;
import com.hcmute.careergraph.enums.candidate.ContactType;
import com.hcmute.careergraph.persistence.models.*;
import com.hcmute.careergraph.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final CompanyRepository companyRepository;
    private final CandidateRepository candidateRepository;
    private final SkillRepository skillRepository;
    private final JobRepository jobRepository;
    private final ApplicationRepository applicationRepository;
    private final JobSkillRepository jobSkillRepository;
    private final Faker faker = new Faker();

    @Transactional
    /*public void seed() {
        if (companyRepository.count() > 0 || candidateRepository.count() > 0 || jobRepository.count() > 0) {
            return;
        }
        // Seed Skills
        List<Skill> skills = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            Skill skill = Skill.builder()
                    .name(faker.job().keySkills())
                    .category(faker.job().field())
                    .description(faker.lorem().sentence())
                    .build();
            skills.add(skill);
        }
        skillRepository.saveAll(skills);

        // Seed Companies
        List<Company> companies = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Company company = Company.builder()
                    .tagname(faker.company().name())
                    .avatar(faker.company().logo())
                    .cover(faker.internet().image())
                    .noOfFollowers(faker.number().numberBetween(10, 1000))
                    .noOfFollowing(faker.number().numberBetween(5, 100))
                    .noOfConnections(faker.number().numberBetween(5, 100))
                    .size(faker.options().option("Small", "Medium", "Large"))
                    .website(faker.internet().url())
                    .ceoName(faker.name().fullName())
                    .noOfMembers(faker.number().numberBetween(10, 1000))
                    .yearFounded(faker.number().numberBetween(1980, 2022))
                    .build();
            // Seed Contact cho Company
            Set<Contact> contacts = new HashSet<>();
            for (int c = 0; c < 2; c++) {
                Contact contact = Contact.builder()
                        .value(faker.internet().emailAddress())
                        .verified(faker.bool().bool())
                        .isPrimary(c == 0)
                        .type(ContactType.EMAIL)
                        .party(company)
                        .build();
                contacts.add(contact);
            }
            company.setContacts(contacts);
            // Seed Address cho Company
            Set<Address> addresses = new HashSet<>();
            for (int a = 0; a < 1; a++) {
                Address address = Address.builder()
                        .name(faker.address().streetAddress())
                        .country(faker.address().country())
                        .province(faker.address().state())
                        .district(faker.address().city())
                        .ward(faker.address().streetName())
                        .isPrimary(true)
                        .party(company)
                        .build();
                addresses.add(address);
            }
            company.setAddresses(addresses);
            companies.add(company);
        }
        companyRepository.saveAll(companies);

        // Seed Candidates
        List<Candidate> candidates = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            Candidate candidate = Candidate.builder()
                    .tagname(faker.name().username())
                    .avatar(faker.avatar().image())
                    .cover(faker.internet().image())
                    .noOfFollowers(faker.number().numberBetween(10, 1000))
                    .noOfFollowing(faker.number().numberBetween(5, 100))
                    .noOfConnections(faker.number().numberBetween(5, 100))
                    .firstName(faker.name().firstName())
                    .lastName(faker.name().lastName())
                    .dateOfBirth(faker.date().birthday(20, 40).toString())
                    .gender(faker.options().option("Male", "Female", "Other"))
                    .currentJobTitle(faker.job().title())
                    .currentCompany(faker.company().name())
                    .industry(faker.job().field())
                    .yearsOfExperience(faker.number().numberBetween(1, 20))
                    .workLocation(faker.address().city())
                    .isOpenToWork(faker.bool().bool())
                    .build();
            // Seed Contact cho Candidate
            Set<Contact> contacts = new HashSet<>();
            for (int c = 0; c < 2; c++) {
                Contact contact = Contact.builder()
                        .value(faker.internet().emailAddress())
                        .verified(faker.bool().bool())
                        .isPrimary(c == 0)
                        .type(ContactType.EMAIL)
                        .party(candidate)
                        .build();
                contacts.add(contact);
            }
            candidate.setContacts(contacts);
            // Seed Address cho Candidate
            Set<Address> addresses = new HashSet<>();
            for (int a = 0; a < 1; a++) {
                Address address = Address.builder()
                        .name(faker.address().streetAddress())
                        .country(faker.address().country())
                        .province(faker.address().state())
                        .district(faker.address().city())
                        .ward(faker.address().streetName())
                        .isPrimary(true)
                        .party(candidate)
                        .build();
                addresses.add(address);
            }
            candidate.setAddresses(addresses);
            candidates.add(candidate);
        }
        candidateRepository.saveAll(candidates);

        // Seed Jobs
        List<Job> jobs = new ArrayList<>();
        Random random = new Random();
        for (int i = 0; i < 80; i++) {
            Company company = companies.get(random.nextInt(companies.size()));
            Job job = Job.builder()
                    .title(faker.job().title())
                    .description(faker.lorem().paragraph())
                    .requirements(faker.lorem().sentence())
                    .benefits(faker.lorem().sentence())
                    .salaryRange(faker.options().option("10-20M", "20-30M", "30-50M"))
                    .experienceLevel(faker.options().option("Fresher", "Junior", "Senior", "Lead"))
                    .workArrangement(faker.options().option("Onsite", "Remote", "Hybrid"))
                    .postedDate(faker.date().past(30, java.util.concurrent.TimeUnit.DAYS).toString())
                    .expiryDate(faker.date().future(60, java.util.concurrent.TimeUnit.DAYS).toString())
                    .numberOfPositions(faker.number().numberBetween(1, 10))
                    .workLocation(faker.address().city())
                    .employmentType(EmploymentType.values()[random.nextInt(EmploymentType.values().length)])
                    .status(Status.ACTIVE)
                    .isUrgent(faker.bool().bool())
                    .jobCategory(i % 2 == 0 ? JobCategory.ENGINEER : JobCategory.BUSINESS)
                    .company(company)
                    .build();
            jobs.add(job);
        }
        jobRepository.saveAll(jobs);

        // Seed JobSkill
        List<JobSkill> jobSkills = new ArrayList<>();
        for (Job job : jobs) {
            Set<Skill> jobSkillSet = new HashSet<>();
            int skillCount = faker.number().numberBetween(3, 6);
            while (jobSkillSet.size() < skillCount) {
                jobSkillSet.add(skills.get(random.nextInt(skills.size())));
            }
            for (Skill skill : jobSkillSet) {
                JobSkill jobSkill = JobSkill.builder()
                        .job(job)
                        .skill(skill)
                        .proficiencyLevel(faker.options().option("Beginner", "Intermediate", "Advanced"))
                        .yearsOfExperience(faker.number().numberBetween(1, 5))
                        .isRequired(faker.bool().bool())
                        .build();
                jobSkills.add(jobSkill);
            }
        }
        jobSkillRepository.saveAll(jobSkills);

        // Seed Applications
        List<Application> applications = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            Candidate candidate = candidates.get(random.nextInt(candidates.size()));
            Job job = jobs.get(random.nextInt(jobs.size()));
            Application application = Application.builder()
                    .coverLetter(faker.lorem().paragraph())
                    .resumeUrl(faker.internet().url())
                    .rating(faker.number().numberBetween(1, 5))
                    .notes(faker.lorem().sentence())
                    .appliedDate(faker.date().past(10, java.util.concurrent.TimeUnit.DAYS).toString())
                    .status(Status.ACTIVE)
                    .candidate(candidate)
                    .job(job)
                    .build();
            applications.add(application);
        }
        applicationRepository.saveAll(applications);
    }*/

    @Override
    public void run(String... args) {

        /**
        List<Job> jobs = jobRepository.findAll();
        Random random = new Random();

        jobs.stream()
                .filter(job -> job.getJobCategory() == null)
                .forEach(job -> {
                    JobCategory[] categories = JobCategory.values();
                    JobCategory randomCategory = categories[random.nextInt(categories.length)];
                    job.setJobCategory(randomCategory);
                });

        jobRepository.saveAll(jobs);*/

        if (companyRepository.count() > 0 || candidateRepository.count() > 0 || jobRepository.count() > 0) {
            return;
        }

        // seed();
    }
}
