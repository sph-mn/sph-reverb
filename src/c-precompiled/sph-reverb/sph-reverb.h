
#ifndef sph_reverb_h_included
#define sph_reverb_h_included

#include <inttypes.h>
#include <stddef.h>

#ifndef sp_sample_t
#define sp_sample_t double
#endif
#ifndef sp_time_t
#define sp_time_t uint32_t
#endif
#ifndef sp_pi
#define sp_pi 3.141592653589793
#endif
#ifndef sp_channel_count_t
#define sp_channel_count_t uint8_t
#endif
#ifndef sph_reverb_position_max_dimensions
#define sph_reverb_position_max_dimensions 8
#endif
typedef struct {
  sp_sample_t* bases;
  sp_channel_count_t channel_count;
  uint8_t basis_length;
} sp_reverb_layout_t;
typedef struct {
  sp_sample_t values[sph_reverb_position_max_dimensions];
  uint8_t dimension_count;
} sp_reverb_position_t;
typedef struct {
  sp_time_t period;
  sp_time_t decay;
  sp_sample_t amplitude;
  sp_time_t phase;
} sp_reverb_mode_t;
typedef struct {
  sp_reverb_mode_t* mode_list;
  sp_time_t mode_count;
} sp_reverb_modal_set_t;
typedef struct {
  sp_reverb_mode_t* mode_list;
  sp_time_t mode_count;
  sp_channel_count_t channel_count;
} sp_reverb_channel_modal_set_t;
typedef struct {
  sp_time_t* delays;
  sp_time_t delay_count;
  sp_sample_t* mix_row_major;
  sp_time_t mix_rows;
  sp_time_t mix_columns;
  sp_time_t* band_periods;
  sp_sample_t* band_gains;
  sp_time_t band_count;
  sp_sample_t strength;
  sp_sample_t* state_directions;
  uint8_t state_dimension_count;
} sp_reverb_late_config_t;
void sp_reverb_complex_divide(sp_sample_t a_real, sp_sample_t a_imag, sp_sample_t b_real, sp_sample_t b_imag, sp_sample_t* out_real, sp_sample_t* out_imag);
sp_sample_t sp_reverb_complex_magnitude(sp_sample_t value_real, sp_sample_t value_imag);
sp_sample_t sp_reverb_complex_argument(sp_sample_t value_real, sp_sample_t value_imag);
sp_reverb_modal_set_t sp_reverb_late_modal(sp_reverb_late_config_t* config);
void sp_reverb_late_modal_residues(sp_reverb_late_config_t* config, sp_reverb_modal_set_t* poles, sp_reverb_layout_t* layout, sp_reverb_position_t* position, sp_reverb_channel_modal_set_t* out_modes);
sp_sample_t sp_reverb_band_gain_at(sp_reverb_late_config_t* config, sp_time_t period);
void sp_reverb_build_feedback_matrix(sp_reverb_late_config_t* config, sp_time_t period, sp_sample_t* matrix_real, sp_sample_t* matrix_imag);
void sp_reverb_build_feedback_matrix_from_polar(sp_reverb_late_config_t* config, sp_sample_t radius, sp_sample_t angle, sp_sample_t* matrix_real, sp_sample_t* matrix_imag);
void sp_reverb_form_identity_minus_feedback(sp_time_t line_count, sp_sample_t* feedback_real, sp_sample_t* feedback_imag, sp_sample_t* a_real, sp_sample_t* a_imag);
void sp_reverb_lower_upper_factorization(sp_time_t line_count, sp_sample_t* matrix_real, sp_sample_t* matrix_imag, sp_time_t* pivot_index_list);
void sp_reverb_lower_upper_solve(sp_time_t line_count, sp_sample_t* matrix_real, sp_sample_t* matrix_imag, sp_time_t* pivot_list, sp_sample_t* right_real, sp_sample_t* right_imag, sp_sample_t* solution_real, sp_sample_t* solution_imag);
void sp_reverb_power_iteration_dominant_eigenpair(sp_time_t line_count, sp_sample_t* matrix_real, sp_sample_t* matrix_imag, sp_sample_t* eigenvalue_real, sp_sample_t* eigenvalue_imag, sp_sample_t* eigenvector_real, sp_sample_t* eigenvector_imag, sp_time_t iteration_limit);
void sp_reverb_eigen_equation_value(sp_reverb_late_config_t* config, sp_sample_t radius, sp_sample_t angle, sp_sample_t* out_real, sp_sample_t* out_imag);
void sp_reverb_eigen_equation_jacobian_finite_difference(sp_reverb_late_config_t* config, sp_sample_t radius, sp_sample_t angle, sp_sample_t* out_real_radius, sp_sample_t* out_real_angle, sp_sample_t* out_imag_radius, sp_sample_t* out_imag_angle);
void sp_reverb_newton_step_on_eigen_equation(sp_sample_t radius, sp_sample_t angle, sp_sample_t real_radius, sp_sample_t real_angle, sp_sample_t imag_radius, sp_sample_t imag_angle, sp_sample_t value_real, sp_sample_t value_imag, sp_sample_t* out_radius_next, sp_sample_t* out_angle_next);
void sp_reverb_build_state_excitation(sp_reverb_late_config_t* config, sp_reverb_position_t* position, sp_sample_t* out_real, sp_sample_t* out_imag);
void sp_reverb_build_state_projection(sp_reverb_late_config_t* config, sp_reverb_layout_t* layout, sp_reverb_position_t* position, sp_channel_count_t channel_index, sp_sample_t* out_real, sp_sample_t* out_imag);
#endif
