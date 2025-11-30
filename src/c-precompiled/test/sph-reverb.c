
#include <stdlib.h>
#include <math.h>
#include <sph-reverb/sph/test.h>
#include <sph-reverb/sph-reverb.h>

#define feq(a, b) (fabs((a - b)) <= 1.0e-12)
#define sp_pi_half 1.5707963267948966
void sp_reverb_late_complex_divide(sp_sample_t a_real, sp_sample_t a_imag, sp_sample_t b_real, sp_sample_t b_imag, sp_sample_t* out_real, sp_sample_t* out_imag);
sp_sample_t sp_reverb_late_complex_magnitude(sp_sample_t value_real, sp_sample_t value_imag);
sp_sample_t sp_reverb_late_complex_argument(sp_sample_t value_real, sp_sample_t value_imag);
sp_sample_t sp_reverb_late_band_gain_at(sp_reverb_late_config_t* config, sp_time_t period);
void sp_reverb_late_build_feedback_matrix(sp_reverb_late_config_t* config, sp_time_t period, sp_sample_t* matrix_real, sp_sample_t* matrix_imag);
void sp_reverb_late_build_feedback_matrix_from_polar(sp_reverb_late_config_t* config, sp_sample_t radius, sp_sample_t angle, sp_sample_t* matrix_real, sp_sample_t* matrix_imag);
void sp_reverb_late_form_identity_minus_feedback(sp_time_t line_count, sp_sample_t* feedback_real, sp_sample_t* feedback_imag, sp_sample_t* a_real, sp_sample_t* a_imag);
void sp_reverb_late_lower_upper_factorization(sp_time_t line_count, sp_sample_t* matrix_real, sp_sample_t* matrix_imag, sp_time_t* pivot_index_list);
void sp_reverb_late_lower_upper_solve(sp_time_t line_count, sp_sample_t* matrix_real, sp_sample_t* matrix_imag, sp_time_t* pivot_list, sp_sample_t* right_real, sp_sample_t* right_imag, sp_sample_t* solution_real, sp_sample_t* solution_imag);
void sp_reverb_late_power_iteration_dominant_eigenpair(sp_time_t line_count, sp_sample_t* matrix_real, sp_sample_t* matrix_imag, sp_sample_t* eigenvalue_real, sp_sample_t* eigenvalue_imag, sp_sample_t* eigenvector_real, sp_sample_t* eigenvector_imag, sp_time_t iteration_limit);
void sp_reverb_late_eigen_equation_value(sp_reverb_late_config_t* config, sp_sample_t radius, sp_sample_t angle, sp_sample_t* out_real, sp_sample_t* out_imag);
void sp_reverb_late_eigen_equation_jacobian_finite_difference(sp_reverb_late_config_t* config, sp_sample_t radius, sp_sample_t angle, sp_sample_t* out_dfr_dr, sp_sample_t* out_dfr_dtheta, sp_sample_t* out_dfi_dr, sp_sample_t* out_dfi_dtheta);
void sp_reverb_late_newton_step_on_eigen_equation(sp_sample_t radius_current, sp_sample_t angle_current, sp_sample_t real_derivative_radius, sp_sample_t real_derivative_angle, sp_sample_t imag_derivative_radius, sp_sample_t imag_derivative_angle, sp_sample_t real_value, sp_sample_t imag_value, sp_sample_t* radius_next, sp_sample_t* angle_next);
void sp_reverb_late_build_state_excitation(sp_reverb_late_config_t* config, sp_reverb_position_t* position, sp_sample_t* out_real, sp_sample_t* out_imag);
void sp_reverb_late_null_vector_of_shifted_matrix(sp_time_t line_count, sp_sample_t* a_real, sp_sample_t* a_imag, int use_transpose, sp_sample_t* out_real, sp_sample_t* out_imag);
void sp_reverb_late_build_state_projection(sp_reverb_late_config_t* config, sp_reverb_layout_t* layout, sp_reverb_position_t* position, sp_channel_count_t channel_index, sp_sample_t* out_real, sp_sample_t* out_imag);
void sp_reverb_late_right_eigenvector_at_pole(sp_reverb_late_config_t* config, sp_sample_t radius, sp_sample_t angle, sp_sample_t* out_real, sp_sample_t* out_imag);
void sp_reverb_late_left_eigenvector_at_pole(sp_reverb_late_config_t* config, sp_sample_t radius, sp_sample_t angle, sp_sample_t* out_real, sp_sample_t* out_imag);
status_t test_sp_reverb_late_complex_primitives(void) {
  status_declare;
  sp_sample_t real_value;
  sp_sample_t imag_value;
  sp_sample_t mag_value;
  sp_sample_t arg_value;
  sp_reverb_late_complex_divide((1.0), (0.0), (1.0), (0.0), (&real_value), (&imag_value));
  test_helper_assert("complex_divide_1_real", (feq(real_value, (1.0))));
  test_helper_assert("complex_divide_1_imag", (feq(imag_value, (0.0))));
  sp_reverb_late_complex_divide((1.0), (1.0), (1.0), (0.0), (&real_value), (&imag_value));
  test_helper_assert("complex_divide_2_real", (feq(real_value, (1.0))));
  test_helper_assert("complex_divide_2_imag", (feq(imag_value, (1.0))));
  mag_value = sp_reverb_late_complex_magnitude((3.0), (4.0));
  test_helper_assert("complex_mag_3_4", (feq(mag_value, (5.0))));
  arg_value = sp_reverb_late_complex_argument((0.0), (1.0));
  test_helper_assert("complex_arg_pi_over_2", (feq(arg_value, sp_pi_half)));
exit:
  status_return;
}
status_t test_sp_reverb_late_feedback_matrix_basic(void) {
  status_declare;
  sp_reverb_late_config_t config;
  sp_time_t delays[1];
  sp_sample_t mix_row_major[1];
  sp_sample_t matrix_real[1];
  sp_sample_t matrix_imag[1];
  sp_sample_t band_gain;
  delays[0] = 100;
  mix_row_major[0] = 1.0;
  config.delays = delays;
  config.delay_count = 1;
  config.mix_row_major = mix_row_major;
  config.mix_rows = 1;
  config.mix_columns = 1;
  config.band_periods = NULL;
  config.band_gains = NULL;
  config.band_count = 0;
  config.strength = 0.5;
  config.state_directions = NULL;
  config.state_dimension_count = 0;
  band_gain = sp_reverb_late_band_gain_at((&config), 200);
  test_helper_assert("band_gain_not_nan", (!isnan(band_gain)));
  sp_reverb_late_build_feedback_matrix((&config), 200, matrix_real, matrix_imag);
  test_helper_assert("feedback_matrix_real_not_nan", (!isnan((matrix_real[0]))));
  test_helper_assert("feedback_matrix_imag_not_nan", (!isnan((matrix_imag[0]))));
  sp_reverb_late_build_feedback_matrix_from_polar((&config), (0.8), (sp_pi * 0.25), matrix_real, matrix_imag);
  test_helper_assert("feedback_matrix_polar_real_not_nan", (!isnan((matrix_real[0]))));
  test_helper_assert("feedback_matrix_polar_imag_not_nan", (!isnan((matrix_imag[0]))));
exit:
  status_return;
}
status_t test_sp_reverb_late_lu_and_solve_basic(void) {
  status_declare;
  sp_sample_t matrix_real[4];
  sp_sample_t matrix_imag[4];
  sp_sample_t right_real[2];
  sp_sample_t right_imag[2];
  sp_sample_t solution_real[2];
  sp_sample_t solution_imag[2];
  sp_time_t pivot_list[2];
  matrix_real[0] = 4.0;
  matrix_real[1] = 1.0;
  matrix_real[2] = 2.0;
  matrix_real[3] = 3.0;
  matrix_imag[0] = 0.0;
  matrix_imag[1] = 0.0;
  matrix_imag[2] = 0.0;
  matrix_imag[3] = 0.0;
  right_real[0] = 1.0;
  right_real[1] = 1.0;
  right_imag[0] = 0.0;
  right_imag[1] = 0.0;
  sp_reverb_late_lower_upper_factorization(2, matrix_real, matrix_imag, pivot_list);
  sp_reverb_late_lower_upper_solve(2, matrix_real, matrix_imag, pivot_list, right_real, right_imag, solution_real, solution_imag);
  test_helper_assert("lu_solve_real_0_not_nan", (!isnan((solution_real[0]))));
  test_helper_assert("lu_solve_real_1_not_nan", (!isnan((solution_real[1]))));
  test_helper_assert("lu_solve_imag_0_not_nan", (!isnan((solution_imag[0]))));
  test_helper_assert("lu_solve_imag_1_not_nan", (!isnan((solution_imag[1]))));
exit:
  status_return;
}
status_t test_sp_reverb_late_state_spatial_basic(void) {
  status_declare;
  sp_reverb_late_config_t config;
  sp_reverb_position_t position;
  sp_reverb_layout_t layout;
  sp_sample_t state_directions[2];
  sp_sample_t bases[2];
  sp_sample_t vec_real[2];
  sp_sample_t vec_imag[2];
  state_directions[0] = -1.0;
  state_directions[1] = 1.0;
  bases[0] = -1.0;
  bases[1] = 1.0;
  config.delays = NULL;
  config.delay_count = 2;
  config.mix_row_major = NULL;
  config.mix_rows = 0;
  config.mix_columns = 0;
  config.band_periods = NULL;
  config.band_gains = NULL;
  config.band_count = 0;
  config.strength = 1.0;
  config.state_directions = state_directions;
  config.state_dimension_count = 1;
  position.dimension_count = 1;
  (position.values)[0] = 0.0;
  layout.bases = bases;
  layout.channel_count = 2;
  layout.basis_length = 1;
  sp_reverb_late_build_state_excitation((&config), (&position), vec_real, vec_imag);
  test_helper_assert("state_excitation_real_0_not_nan", (!isnan((vec_real[0]))));
  test_helper_assert("state_excitation_real_1_not_nan", (!isnan((vec_real[1]))));
  test_helper_assert("state_excitation_imag_0_zero", (feq((vec_imag[0]), (0.0))));
  test_helper_assert("state_excitation_imag_1_zero", (feq((vec_imag[1]), (0.0))));
  sp_reverb_late_build_state_projection((&config), (&layout), (&position), 0, vec_real, vec_imag);
  test_helper_assert("state_projection_real_0_not_nan", (!isnan((vec_real[0]))));
  test_helper_assert("state_projection_real_1_not_nan", (!isnan((vec_real[1]))));
  test_helper_assert("state_projection_imag_0_zero", (feq((vec_imag[0]), (0.0))));
  test_helper_assert("state_projection_imag_1_zero", (feq((vec_imag[1]), (0.0))));
exit:
  status_return;
}
status_t test_sp_reverb_late_modal_basic(void) {
  status_declare;
  sp_reverb_late_config_t config;
  sp_reverb_late_modal_set_t poles;
  sp_time_t delays[2];
  sp_sample_t mix_row_major[4];
  sp_sample_t state_directions[2];
  delays[0] = 100;
  delays[1] = 150;
  mix_row_major[0] = 1.0;
  mix_row_major[1] = 0.0;
  mix_row_major[2] = 0.0;
  mix_row_major[3] = 1.0;
  state_directions[0] = -1.0;
  state_directions[1] = 1.0;
  config.delays = delays;
  config.delay_count = 2;
  config.mix_row_major = mix_row_major;
  config.mix_rows = 2;
  config.mix_columns = 2;
  config.band_periods = NULL;
  config.band_gains = NULL;
  config.band_count = 0;
  config.strength = 0.8;
  config.state_directions = state_directions;
  config.state_dimension_count = 1;
  poles = sp_reverb_late_modal((&config));
  test_helper_assert("modal_mode_list_not_null", (poles.mode_list != NULL));
  test_helper_assert("modal_mode_count_positive", (poles.mode_count > 0));
  test_helper_assert("modal_period_positive", (((poles.mode_list)[0]).period > 0));
  test_helper_assert("modal_decay_positive", (((poles.mode_list)[0]).decay > 0));
  free((poles.mode_list));
exit:
  status_return;
}
status_t test_sp_reverb_late_modal_residues_basic(void) {
  status_declare;
  sp_reverb_late_config_t config;
  sp_reverb_layout_t layout;
  sp_reverb_position_t position;
  sp_reverb_late_modal_set_t poles;
  sp_reverb_late_channel_modal_set_t channel_modes;
  sp_time_t delays[2];
  sp_sample_t mix_row_major[4];
  sp_sample_t state_directions[2];
  sp_sample_t bases[2];
  sp_time_t total_modes;
  delays[0] = 100;
  delays[1] = 150;
  mix_row_major[0] = 1.0;
  mix_row_major[1] = 0.0;
  mix_row_major[2] = 0.0;
  mix_row_major[3] = 1.0;
  state_directions[0] = -1.0;
  state_directions[1] = 1.0;
  bases[0] = -1.0;
  bases[1] = 1.0;
  config.delays = delays;
  config.delay_count = 2;
  config.mix_row_major = mix_row_major;
  config.mix_rows = 2;
  config.mix_columns = 2;
  config.band_periods = NULL;
  config.band_gains = NULL;
  config.band_count = 0;
  config.strength = 0.8;
  config.state_directions = state_directions;
  config.state_dimension_count = 1;
  layout.bases = bases;
  layout.channel_count = 2;
  layout.basis_length = 1;
  position.dimension_count = 1;
  (position.values)[0] = 0.0;
  poles = sp_reverb_late_modal((&config));
  test_helper_assert("modal_res_poles_not_null", (poles.mode_list != NULL));
  test_helper_assert("modal_res_poles_count_positive", (poles.mode_count > 0));
  channel_modes.mode_list = NULL;
  channel_modes.mode_count = 0;
  channel_modes.channel_count = 0;
  sp_reverb_late_modal_residues((&config), (&poles), (&layout), (&position), (&channel_modes));
  test_helper_assert("modal_res_mode_list_not_null", (channel_modes.mode_list != NULL));
  test_helper_assert("modal_res_channel_count_correct", (channel_modes.channel_count == layout.channel_count));
  test_helper_assert("modal_res_mode_count_correct", (channel_modes.mode_count == poles.mode_count));
  total_modes = (channel_modes.mode_count * channel_modes.channel_count);
  test_helper_assert("modal_res_total_modes_positive", (total_modes > 0));
  test_helper_assert("modal_res_period_match", (((channel_modes.mode_list)[0]).period == ((poles.mode_list)[0]).period));
  test_helper_assert("modal-res-decay-match", (((channel_modes.mode_list)[0]).decay == ((poles.mode_list)[0]).decay));
  free((channel_modes.amplitude_list));
  free((channel_modes.phase_list));
  free((poles.mode_list));
exit:
  status_return;
}
int main(void) {
  status_declare;
  test_helper_test_one(test_sp_reverb_late_modal_residues_basic);
  test_helper_test_one(test_sp_reverb_late_complex_primitives);
  test_helper_test_one(test_sp_reverb_late_feedback_matrix_basic);
  test_helper_test_one(test_sp_reverb_late_lu_and_solve_basic);
  test_helper_test_one(test_sp_reverb_late_state_spatial_basic);
  test_helper_test_one(test_sp_reverb_late_modal_basic);
  test_helper_display_summary;
exit:
  return ((status.id));
}
